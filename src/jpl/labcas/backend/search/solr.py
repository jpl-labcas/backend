"""Solr-backed search engine implementation."""

from __future__ import annotations

import logging
from typing import Any, Dict

import httpx

from ..config import Settings, get_settings
from .base import SearchEngine, SearchResult

LOG = logging.getLogger(__name__)


class SolrSearchEngine(SearchEngine):
    """Minimal Solr client used by the service layer."""

    def __init__(self, settings: Settings | None = None, client: httpx.AsyncClient | None = None) -> None:
        self.settings = settings or get_settings()
        if not self.settings.solr_url:
            msg = "SOLR_URL configuration is required for the Solr search engine."
            raise ValueError(msg)

        verify = self.settings.solr_verify_ssl
        self.client = client or httpx.AsyncClient(base_url=str(self.settings.solr_url), verify=verify)

    async def query(self, core: str, params: Dict[str, Any]) -> SearchResult:
        query_params = dict(params)
        query_params.setdefault("wt", "json")
        response = await self.client.get(f"/{core}/select", params=query_params)
        response.raise_for_status()
        payload = response.json()
        docs = payload.get("response", {}).get("docs", [])
        num_found = payload.get("response", {}).get("numFound", 0)
        LOG.debug("Solr query core=%s params=%s returned %s docs", core, params, num_found)
        return SearchResult(documents=docs, total=num_found)

    async def update(self, core: str, payload: Dict[str, Any]) -> None:
        response = await self.client.post(f"/{core}/update", json=payload)
        response.raise_for_status()
        LOG.debug("Solr update to core=%s succeeded", core)


