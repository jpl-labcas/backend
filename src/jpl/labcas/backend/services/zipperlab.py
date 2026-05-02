"""Service for initiating ZIP creation requests with Zipperlab."""

from __future__ import annotations

import logging
from functools import lru_cache
from typing import Iterable, Sequence
from urllib.parse import unquote_plus

import httpx

from ..auth.dependencies import SecurityContext
from ..config import Settings, get_settings
from .download import SOLR_FIELD_FILE_LOCATION, SOLR_FIELD_FILE_NAME, SOLR_FIELD_NAME
from .query import QueryService

LOG = logging.getLogger(__name__)


class ZipperlabService:
    """Resolve LabCAS files and forward ZIP requests to Zipperlab."""

    def __init__(
        self,
        *,
        settings: Settings | None = None,
        query_service: QueryService | None = None,
        client: httpx.AsyncClient | None = None,
    ) -> None:
        self.settings = settings or get_settings()
        self._query_service = query_service
        self.client = client or httpx.AsyncClient(verify=False)

    @property
    def query_service(self) -> QueryService:
        """Lazy initialization of the query service."""

        if self._query_service is None:
            self._query_service = QueryService(settings=self.settings)
        return self._query_service

    async def resolve_file_paths(
        self,
        *,
        security: SecurityContext,
        query: str | None,
        ids: Sequence[str],
    ) -> list[str]:
        """Resolve a query or explicit file IDs into authorized filesystem paths."""

        if query and ids:
            raise ValueError("Specify either query or id values, not both.")
        if query:
            return await self._resolve_query_file_paths(
                security=security,
                query=self._decode_frontend_value(query),
            )
        if ids:
            return await self._resolve_id_file_paths(
                security=security,
                ids=[self._decode_frontend_value(file_id) for file_id in ids],
            )
        raise ValueError("Specify either a query or at least one id value.")

    async def initiate_zip(self, *, email: str, files: Sequence[str]) -> str:
        """Initiate ZIP creation and return the UUID from Zipperlab."""

        if not self.settings.zipperlab_url:
            raise ValueError("LABCAS_ZIPPERLAB_URL configuration is required for /zip.")
        if not files:
            raise ValueError("No files matched the ZIP request.")

        payload = {
            "operation": "initiate",
            "email": email,
            "files": list(files),
        }
        LOG.info("Initiating Zipperlab request for email=%s file_count=%s", email, len(files))
        response = await self.client.post(str(self.settings.zipperlab_url), json=payload)
        response.raise_for_status()
        uuid = response.text.splitlines()[0].strip() if response.text else ""
        if not uuid:
            raise ValueError("Zipperlab returned an empty UUID.")
        LOG.info("Zipperlab initiated request uuid=%s", uuid)
        return uuid

    async def _resolve_query_file_paths(
        self,
        *,
        security: SecurityContext,
        query: str,
    ) -> list[str]:
        params = {
            "q": query,
            "fl": f"{SOLR_FIELD_FILE_LOCATION},{SOLR_FIELD_FILE_NAME},{SOLR_FIELD_NAME}",
            "rows": self.settings.solr_max_rows,
        }
        result = await self.query_service.query_files(security=security, params=params)
        docs = result.get("response", {}).get("docs", [])
        return self._file_paths_from_docs(docs)

    async def _resolve_id_file_paths(
        self,
        *,
        security: SecurityContext,
        ids: Sequence[str],
    ) -> list[str]:
        escaped_ids = [self._escape_solr_value(file_id) for file_id in ids if file_id]
        if not escaped_ids:
            return []

        quoted_ids = " OR ".join(f'"{file_id}"' for file_id in escaped_ids)
        params = {
            "q": f"id:({quoted_ids})",
            "fl": f"id,{SOLR_FIELD_FILE_LOCATION},{SOLR_FIELD_FILE_NAME},{SOLR_FIELD_NAME}",
            "rows": min(len(escaped_ids), self.settings.solr_max_rows),
        }
        result = await self.query_service.query_files(security=security, params=params)
        docs = result.get("response", {}).get("docs", [])
        return self._file_paths_from_docs(docs)

    @staticmethod
    def _escape_solr_value(value: str) -> str:
        return value.replace("\\", "\\\\").replace('"', '\\"')

    @staticmethod
    def _decode_frontend_value(value: str) -> str:
        """Mirror the Java service workaround for double URL-encoded form values."""

        return unquote_plus(value)

    @classmethod
    def _file_paths_from_docs(cls, docs: Iterable[dict]) -> list[str]:
        file_paths: list[str] = []
        for doc in docs:
            file_path = cls._file_path_from_doc(doc)
            if file_path:
                file_paths.append(file_path)
        return file_paths

    @staticmethod
    def _file_path_from_doc(doc: dict) -> str | None:
        file_location = doc.get(SOLR_FIELD_FILE_LOCATION)
        file_name = doc.get(SOLR_FIELD_FILE_NAME)
        real_file_name = file_name

        name_field = doc.get(SOLR_FIELD_NAME)
        if isinstance(name_field, list) and name_field and isinstance(name_field[0], str) and name_field[0]:
            real_file_name = name_field[0]
        elif isinstance(name_field, str) and name_field:
            real_file_name = name_field

        if not isinstance(file_location, str) or not file_location:
            return None
        if not isinstance(real_file_name, str) or not real_file_name:
            return None
        return f"{file_location}/{real_file_name}"


@lru_cache(maxsize=1)
def _cached_zipperlab_service() -> ZipperlabService:
    return ZipperlabService()


def get_zipperlab_service() -> ZipperlabService:
    """FastAPI dependency hook for the Zipperlab service."""

    return _cached_zipperlab_service()

