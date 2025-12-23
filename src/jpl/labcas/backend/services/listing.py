"""Services for generating LabCAS download lists."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from functools import lru_cache
from typing import Any, Iterable, Sequence
from urllib.parse import quote

from ..auth.dependencies import SecurityContext
from ..config import Settings, get_settings
from ..search import SearchEngine, SolrSearchEngine
from ..utils.security import ensure_safe_value

LOG = logging.getLogger(__name__)


@dataclass(slots=True)
class ListRequest:
    """Captures the parameters required to query Solr."""

    query: str
    filters: list[str]
    start: int
    rows: int


class ListService:
    """Encapsulates the logic required to build LabCAS list responses."""

    COLLECTIONS_CORE = "collections"
    DATASETS_CORE = "datasets"
    FILES_CORE = "files"
    COLLECTION_FIELD = "CollectionId"
    DATASET_FIELD = "DatasetId"
    FILE_ID_FIELD = "id"
    DEFAULT_ROWS = 10

    def __init__(self, *, search_engine: SearchEngine | None = None, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.search_engine = search_engine or SolrSearchEngine(settings=self.settings)

    async def list_collections(
        self,
        *,
        security: SecurityContext,
        query: str,
        filters: Sequence[str],
        start: int,
        rows: int,
    ) -> str:
        """Return download URLs for files in collections that satisfy the given query."""

        request = self._build_pass_through_request(
            security=security,
            query=query,
            filters=filters,
            start=start,
            rows=rows,
        )
        params = self._to_solr_params(request)
        search_result = await self.search_engine.query(self.COLLECTIONS_CORE, params)
        collection_ids = self._extract_ids(search_result.documents)
        LOG.debug("Collections query matched %s ids", len(collection_ids))
        if not collection_ids:
            return ""

        return await self._execute_files_query(self.COLLECTION_FIELD, collection_ids)

    async def list_datasets(
        self,
        *,
        security: SecurityContext,
        query: str,
        filters: Sequence[str],
        start: int,
        rows: int,
    ) -> str:
        """Return download URLs for files in datasets that satisfy the given query."""

        request = self._build_pass_through_request(
            security=security,
            query=query,
            filters=filters,
            start=start,
            rows=rows,
        )
        params = self._to_solr_params(request)
        search_result = await self.search_engine.query(self.DATASETS_CORE, params)
        dataset_ids = self._extract_ids(search_result.documents)
        LOG.debug("Datasets query matched %s ids", len(dataset_ids))
        if not dataset_ids:
            return ""

        return await self._execute_files_query(self.DATASET_FIELD, dataset_ids)

    async def list_files(
        self,
        *,
        security: SecurityContext,
        query: str,
        filters: Sequence[str],
        start: int,
        rows: int,
    ) -> str:
        """Return download URLs for files that satisfy the given query directly."""

        request = self._build_pass_through_request(
            security=security,
            query=query,
            filters=filters,
            start=start,
            rows=rows,
        )
        params = self._to_solr_params(request)
        params.setdefault("fl", self.FILE_ID_FIELD)
        params.setdefault("sort", f"{self.FILE_ID_FIELD} desc")
        search_result = await self.search_engine.query(self.FILES_CORE, params)
        file_ids = self._extract_ids(search_result.documents)
        LOG.debug("Files direct query matched %s ids", len(file_ids))
        return self._build_urls_from_ids(file_ids)

    # Internal helpers -------------------------------------------------

    def _build_pass_through_request(
        self,
        *,
        security: SecurityContext,
        query: str,
        filters: Sequence[str],
        start: int,
        rows: int,
    ) -> ListRequest:
        safe_query = self._sanitize_query(query) or "*:*"
        safe_filters = self._sanitize_filters(filters)
        ac_filter = self._build_access_control_filter(security)
        if ac_filter:
            safe_filters.append(ac_filter)

        normalized_rows = self._normalize_rows(rows)
        return ListRequest(
            query=safe_query,
            filters=safe_filters,
            start=max(start, 0),
            rows=normalized_rows,
        )

    @staticmethod
    def _to_solr_params(request: ListRequest) -> dict[str, Any]:
        params: dict[str, Any] = {
            "q": request.query,
            "start": request.start,
            "rows": request.rows,
        }
        if request.filters:
            params["fq"] = request.filters
        return params

    async def _execute_files_query(
        self,
        field_name: str,
        field_values: Sequence[str],
    ) -> str:
        """Iteratively query the files core and build download URLs."""

        safe_values = self._sanitize_filters(field_values)
        if not safe_values:
            return ""

        query = self._build_or_query(field_name, safe_values)
        rows = min(100, self.settings.solr_max_rows)
        start = 0
        total = 1
        identifiers: list[str] = []

        while start < total:
            params = {
                "q": query,
                "fl": self.FILE_ID_FIELD,
                "rows": rows,
                "start": start,
                "sort": f"{self.FILE_ID_FIELD} desc",
            }
            search_result = await self.search_engine.query(self.FILES_CORE, params)
            docs = search_result.documents
            total = search_result.total
            LOG.debug(
                "Files query %s returned %s/%s documents",
                field_name,
                len(docs),
                total,
            )
            if not docs:
                break

            start += len(docs)
            identifiers.extend(doc.get(self.FILE_ID_FIELD) for doc in docs if self.FILE_ID_FIELD in doc)

            if start >= total:
                break

        return self._build_urls_from_ids(identifiers)

    def _build_access_control_filter(self, security: SecurityContext) -> str | None:
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

    @staticmethod
    def _build_or_query(field_name: str, values: Sequence[str]) -> str:
        quoted = " OR ".join(f'"{value}"' for value in values)
        return f"{field_name}:({quoted})"

    @staticmethod
    def _extract_ids(documents: Sequence[dict]) -> list[str]:
        ids: list[str] = []
        for doc in documents:
            identifier = doc.get("id")
            if isinstance(identifier, str):
                ids.append(identifier)
        return ids

    @staticmethod
    def _sanitize_query(value: str | None) -> str:
        if value is None:
            return ""
        return ensure_safe_value(value.strip())

    @staticmethod
    def _sanitize_filters(values: Iterable[str] | None) -> list[str]:
        sanitized: list[str] = []
        if not values:
            return sanitized
        for value in values:
            if value is None:
                continue
            sanitized.append(ensure_safe_value(value.strip()))
        return sanitized

    def _normalize_rows(self, rows: int) -> int:
        if rows <= 0:
            rows = self.DEFAULT_ROWS
        return min(rows, self.settings.solr_max_rows)

    def _build_download_url(self, identifier: str | None) -> str | None:
        if not identifier:
            return None
        encoded = quote(identifier, safe="")
        return f"{self.settings.download_base_url}?id={encoded}"

    def _build_urls_from_ids(self, identifiers: Iterable[str | None]) -> str:
        urls = [self._build_download_url(identifier) for identifier in identifiers if identifier]
        cleaned = [url for url in urls if url]
        if not cleaned:
            return ""
        return "\n".join(cleaned) + "\n"


@lru_cache(maxsize=1)
def _cached_list_service() -> ListService:
    return ListService()


def get_list_service() -> ListService:
    """FastAPI dependency hook for the list service."""

    return _cached_list_service()


