"""Service for proxying Solr query requests with access control."""

from __future__ import annotations

import logging
from functools import lru_cache
from typing import Any

import httpx

from ..auth.dependencies import SecurityContext
from ..config import Settings, get_settings
from ..utils.security import ensure_safe_value

LOG = logging.getLogger(__name__)


class QueryService:
    """Encapsulates the logic required to proxy Solr queries with access control."""

    COLLECTIONS_CORE = "collections"
    DATASETS_CORE = "datasets"
    FILES_CORE = "files"

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
        ac_filter = self._build_access_control_filter(security)
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

        # Execute query and return full Solr response
        response = await self.client.get(f"/{core}/select", params=safe_params)
        response.raise_for_status()
        solr_response = response.json()
        LOG.debug("Solr query core=%s params=%s returned response", core, safe_params)
        return solr_response

    def _build_access_control_filter(self, security: SecurityContext) -> str | None:
        """Build the access control filter query string."""

        super_owner = (self.settings.super_owner_principal or "").strip()
        if super_owner and super_owner in security.groups:
            return None

        principals = []
        if self.settings.public_owner_principal:
            principals.append(self.settings.public_owner_principal.strip())
        principals.extend(security.groups)
        principals = [p for p in principals if p]

        if not principals:
            return None

        unique = list(dict.fromkeys(principals))
        joined = " OR ".join(f'"{principal}"' for principal in unique)
        return f"OwnerPrincipal:({joined})"

    def _sanitize_params(self, params: dict[str, Any]) -> dict[str, Any]:
        """Sanitize query parameters to prevent unsafe characters."""

        safe_params: dict[str, Any] = {}

        for key, value in params.items():
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


@lru_cache(maxsize=1)
def _cached_query_service() -> QueryService:
    return QueryService()


def get_query_service() -> QueryService:
    """FastAPI dependency hook for the query service."""

    return _cached_query_service()

