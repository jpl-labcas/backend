"""Search engine abstractions."""

from .base import SearchEngine
from .solr import SolrSearchEngine

__all__ = ["SearchEngine", "SolrSearchEngine"]


