"""Base abstraction for search engines."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, List, Protocol


@dataclass(slots=True)
class SearchResult:
    """Represents a generic search result."""

    documents: List[Dict[str, Any]]
    total: int


class SearchEngine(Protocol):
    """Protocol for search engine implementations."""

    async def query(self, core: str, params: Dict[str, Any]) -> SearchResult:
        """Execute a query and return the results."""

    async def update(self, core: str, payload: Dict[str, Any]) -> None:
        """Submit an update to the search index."""


