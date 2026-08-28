"""Service for proxying Solr query requests with access control."""

from __future__ import annotations

import logging
from functools import lru_cache
from typing import Any, Sequence

import httpx, json

from ..auth.dependencies import SecurityContext
from ..config import Settings, get_settings
from ..utils.security import ensure_safe_value
from .access_control import build_owner_principal_filter

LOG = logging.getLogger(__name__)


class QueryService:
    """Encapsulates the logic required to proxy Solr queries with access control."""

    COLLECTIONS_CORE = "collections"
    DATASETS_CORE = "datasets"
    FILES_CORE = "files"

    #: Parameters that could be used for SSRF attacks — these will be filtered out.
    DANGEROUS_PARAMETERS = (
        "shards",       # Can point to arbitrary Solr servers
        "shards.qt",    # Can specify query template on remote shards
        "stream.url",   # Can stream from arbitrary URLs
        "stream.file",  # Can read arbitrary files
        "stream.body",  # Can execute arbitrary code
        "expr",         # Streaming expressions can open connections to arbitrary hosts
    )

    #: Parameter name suffixes that could be used for SSRF attacks (e.g. ``terms.shards``).
    DANGEROUS_SUFFIXES = (
        ".shards",      # Per-component shard targeting can point to arbitrary Solr servers
    )

    #: Stay well under Jetty's default max form size (~200KB) and Solr's maxBooleanClauses (1024).
    MAX_SOLR_QUERY_CHARS = 32_768
    MAX_SOLR_BOOLEAN_CLAUSES = 512

    def __init__(self, *, settings: Settings | None = None, client: httpx.AsyncClient | None = None) -> None:
        self.settings = settings or get_settings()
        
        # Only require solr_url if we need to create a client
        if client is None and not self.settings.solr_url:
            msg = "SOLR_URL configuration is required for the query service when no client is provided."
            raise ValueError(msg)

        verify = self.settings.solr_verify_ssl
        self.client = client or httpx.AsyncClient(base_url=str(self.settings.solr_url), verify=verify)

    async def query_collections(
        self,
        *,
        security: SecurityContext,
        params: dict[str, Any],
    ) -> dict[str, Any]:
        """Query the collections core with access control applied."""

        return await self._query_core(self.COLLECTIONS_CORE, security=security, params=params)

    async def query_datasets(
        self,
        *,
        security: SecurityContext,
        params: dict[str, Any],
    ) -> dict[str, Any]:
        """Query the datasets core with access control applied."""

        return await self._query_core(self.DATASETS_CORE, security=security, params=params)

    async def query_files(
        self,
        *,
        security: SecurityContext,
        params: dict[str, Any],
    ) -> dict[str, Any]:
        """Query the files core with access control applied."""

        return await self._query_core(self.FILES_CORE, security=security, params=params)

    async def _query_core(
        self,
        core: str,
        *,
        security: SecurityContext,
        params: dict[str, Any],
    ) -> dict[str, Any]:
        """Execute a query against a Solr core with access control."""

        # Validate and sanitize parameters
        safe_params = self._sanitize_params(params)

        # Validate rows limit
        rows = safe_params.get("rows")
        if rows is not None:
            try:
                rows_int = int(rows) if isinstance(rows, str) else rows
                if rows_int > self.settings.solr_max_rows:
                    raise ValueError(f"rows must be ≤ {self.settings.solr_max_rows}")
            except (ValueError, TypeError) as exc:
                if isinstance(exc, ValueError) and "must be ≤" in str(exc):
                    raise
                raise ValueError("rows must be a valid integer") from exc

        # Add access control filter
        ac_filter = build_owner_principal_filter(self.settings, security)
        LOG.info('🔒 The access control filter is %r', ac_filter)
        if ac_filter:
            # Add to existing fq parameters
            existing_fq = safe_params.get("fq", [])
            if isinstance(existing_fq, str):
                existing_fq = [existing_fq]
            elif not isinstance(existing_fq, list):
                existing_fq = []
            existing_fq.append(ac_filter)
            safe_params["fq"] = existing_fq

        # Ensure JSON response format
        safe_params.setdefault("wt", "json")

        batches = self._solr_param_batches(safe_params)
        responses = [
            await self._post_select(core, batch_params, batch_index=index, batch_count=len(batches))
            for index, batch_params in enumerate(batches, start=1)
        ]
        return self._merge_solr_responses(responses)

    def _sanitize_params(self, params: dict[str, Any]) -> dict[str, Any]:
        """Sanitize query parameters to prevent unsafe characters."""

        safe_params: dict[str, Any] = {}

        for key, value in params.items():
            if self._is_dangerous_param(key):
                LOG.warning("⚠️ Blocked dangerous parameter: %s", key)
                continue
            if key in ("q", "fq", "fl", "sort", "q.op", "df", "wt"):
                if isinstance(value, str):
                    # For query strings (q, fq), allow quotes as they're part of Solr syntax
                    # Only validate that the query doesn't contain truly unsafe characters
                    if key in ("q", "fq"):
                        # Allow quotes in Solr query strings, but check for other unsafe chars
                        # Remove quotes temporarily for validation, then restore
                        temp_value = value.replace('"', "").replace("'", "")
                        ensure_safe_value(temp_value)
                        safe_params[key] = value
                    else:
                        safe_value = ensure_safe_value(value)
                        safe_params[key] = safe_value
                elif isinstance(value, list):
                    # For filter queries (fq), allow quotes
                    if key == "fq":
                        safe_params[key] = value
                    else:
                        safe_params[key] = [ensure_safe_value(str(v)) if isinstance(v, str) else v for v in value]
                else:
                    safe_params[key] = value
            elif key in ("start", "rows"):
                # Numeric parameters
                try:
                    safe_params[key] = int(value) if isinstance(value, str) else value
                except (ValueError, TypeError):
                    # Keep original value, validation will catch it later
                    safe_params[key] = value
            else:
                # Pass through other parameters (Solr supports many)
                if isinstance(value, str):
                    safe_value = ensure_safe_value(value)
                    safe_params[key] = safe_value
                elif isinstance(value, list):
                    safe_params[key] = [ensure_safe_value(str(v)) if isinstance(v, str) else v for v in value]
                else:
                    safe_params[key] = value

        return safe_params

    async def _post_select(
        self,
        core: str,
        safe_params: dict[str, Any],
        *,
        batch_index: int,
        batch_count: int,
    ) -> dict[str, Any]:
        """POST a select query to Solr and return the JSON body."""

        payload_size = len(json.dumps(safe_params))
        if batch_count == 1:
            LOG.info("ABOUT TO CALL SOLR with safe_params of %d characters", payload_size)
        else:
            LOG.info(
                "ABOUT TO CALL SOLR batch %d/%d with safe_params of %d characters",
                batch_index,
                batch_count,
                payload_size,
            )
        response = await self.client.post(f"/{core}/select", data=safe_params)
        response.raise_for_status()
        solr_response = response.json()
        LOG.info("SOLR query returned %d documents", len(solr_response.get("response", {}).get("docs", [])))
        return solr_response

    def _solr_param_batches(self, safe_params: dict[str, Any]) -> list[dict[str, Any]]:
        """Split oversized OR-group queries into Solr-safe batches."""

        query = safe_params.get("q")
        parsed = self._parse_or_group_query(query) if isinstance(query, str) else None
        if parsed is None:
            return [safe_params]

        prefix, clauses, suffix = parsed
        if not self._should_batch_query(safe_params, clauses):
            return [safe_params]

        original_rows = self._as_int(safe_params.get("rows"), default=0)
        batches: list[dict[str, Any]] = []
        for group in self._chunk_or_clauses(prefix, clauses, suffix):
            batch = dict(safe_params)
            batch["q"] = f"{prefix}{' OR '.join(group)}{suffix}"
            batch["start"] = 0
            batch["rows"] = min(self.settings.solr_max_rows, max(original_rows, len(group)))
            batches.append(batch)

        LOG.info("Splitting Solr query into %d batches (%d OR clauses)", len(batches), len(clauses))
        return batches

    def _should_batch_query(self, safe_params: dict[str, Any], clauses: list[str]) -> bool:
        query = safe_params.get("q")
        if len(clauses) > min(self.MAX_SOLR_BOOLEAN_CLAUSES, self.settings.solr_max_rows):
            return True
        if isinstance(query, str) and len(query) > self.MAX_SOLR_QUERY_CHARS:
            return True
        return len(json.dumps(safe_params)) > self.MAX_SOLR_QUERY_CHARS

    def _chunk_or_clauses(self, prefix: str, clauses: Sequence[str], suffix: str) -> list[list[str]]:
        max_chars = self.MAX_SOLR_QUERY_CHARS
        max_clauses = max(1, min(self.MAX_SOLR_BOOLEAN_CLAUSES, self.settings.solr_max_rows))
        wrapper_len = len(prefix) + len(suffix)
        or_len = len(" OR ")
        chunks: list[list[str]] = []
        current: list[str] = []
        current_body_len = 0

        for clause in clauses:
            added_len = len(clause) if not current else or_len + len(clause)
            exceeds_chars = bool(current) and wrapper_len + current_body_len + added_len > max_chars
            exceeds_clauses = bool(current) and len(current) >= max_clauses
            if exceeds_chars or exceeds_clauses:
                chunks.append(current)
                current = []
                current_body_len = 0
                added_len = len(clause)
            current.append(clause)
            current_body_len += added_len

        if current:
            chunks.append(current)
        return chunks

    @staticmethod
    def _parse_or_group_query(query: str) -> tuple[str, list[str], str] | None:
        """Parse ``field:(term OR term …)`` into prefix, clauses, and suffix."""

        if " OR " not in query:
            return None
        open_paren = query.find("(")
        if open_paren == -1 or not query.endswith(")"):
            return None
        prefix = query[: open_paren + 1]
        body = query[open_paren + 1 : -1]
        clauses = QueryService._split_or_clauses(body)
        if len(clauses) < 2:
            return None
        return prefix, clauses, ")"

    @staticmethod
    def _split_or_clauses(body: str) -> list[str]:
        """Split a Solr OR list on top-level `` OR ``, ignoring quoted delimiters."""

        clauses: list[str] = []
        current: list[str] = []
        in_quotes = False
        escaped = False
        index = 0
        while index < len(body):
            char = body[index]
            if escaped:
                current.append(char)
                escaped = False
            elif char == "\\" and in_quotes:
                current.append(char)
                escaped = True
            elif char == '"':
                in_quotes = not in_quotes
                current.append(char)
            elif not in_quotes and body.startswith(" OR ", index):
                clause = "".join(current).strip()
                if clause:
                    clauses.append(clause)
                current = []
                index += 4
                continue
            else:
                current.append(char)
            index += 1
        clause = "".join(current).strip()
        if clause:
            clauses.append(clause)
        return clauses

    @staticmethod
    def _merge_solr_responses(responses: list[dict[str, Any]]) -> dict[str, Any]:
        """Combine batched Solr responses into a single select result."""

        if len(responses) == 1:
            return responses[0]

        docs: list[Any] = []
        num_found = 0
        for item in responses:
            payload = item.get("response") or {}
            docs.extend(payload.get("docs") or [])
            num_found += int(payload.get("numFound") or 0)

        merged = dict(responses[0])
        first_payload = dict(responses[0].get("response") or {})
        first_payload["docs"] = docs
        first_payload["numFound"] = num_found
        first_payload["start"] = 0
        merged["response"] = first_payload
        return merged

    @staticmethod
    def _as_int(value: Any, *, default: int) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return default

    @classmethod
    def _is_dangerous_param(cls, key: str) -> bool:
        """Return True if the parameter could be used for SSRF attacks.

        Matches the exact parameter name, any sub-parameter of it
        (e.g. ``shards`` also blocks ``shards.tolerant``), or any parameter
        ending in a dangerous suffix (e.g. ``terms.shards``).
        """
        if any(key == dangerous or key.startswith(dangerous + ".") for dangerous in cls.DANGEROUS_PARAMETERS):
            return True
        return any(key.endswith(suffix) for suffix in cls.DANGEROUS_SUFFIXES)


@lru_cache(maxsize=1)
def _cached_query_service() -> QueryService:
    return QueryService()


def get_query_service() -> QueryService:
    """FastAPI dependency hook for the query service."""

    return _cached_query_service()

