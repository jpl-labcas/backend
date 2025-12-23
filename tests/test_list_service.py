"""Unit tests for list service."""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock

import pytest

from jpl.labcas.backend.auth.dependencies import SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.listing import ListService
from jpl.labcas.backend.search.base import SearchResult


@pytest.fixture
def test_settings() -> Settings:
    """Create test settings."""
    return Settings(
        solr_url="http://localhost:8983/solr",
        solr_max_rows=1000,
        download_base_url="http://localhost:8000/data-access-api/download",
        public_owner_principal="public",
        super_owner_principal=None,
    )


@pytest.fixture
def mock_search_engine() -> MagicMock:
    """Create a mock search engine."""
    engine = MagicMock()
    engine.query = AsyncMock(return_value=SearchResult(documents=[], total=0))
    return engine


@pytest.mark.asyncio
async def test_list_collections_empty_result(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test list_collections with empty result."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.list_collections(
        security=security,
        query="*:*",
        filters=[],
        start=0,
        rows=10,
    )
    
    assert result == ""


@pytest.mark.asyncio
async def test_list_collections_with_results(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test list_collections with results."""
    # Mock collections query
    collections_result = SearchResult(
        documents=[{"id": "collection1"}, {"id": "collection2"}],
        total=2,
    )
    
    # Mock files query
    files_result = SearchResult(
        documents=[{"id": "file1"}, {"id": "file2"}],
        total=2,
    )
    
    mock_search_engine.query = AsyncMock(side_effect=[collections_result, files_result])
    
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.list_collections(
        security=security,
        query="*:*",
        filters=[],
        start=0,
        rows=10,
    )
    
    assert "file1" in result
    assert "file2" in result
    assert "data-access-api/download?id=" in result


@pytest.mark.asyncio
async def test_list_datasets_empty_result(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test list_datasets with empty result."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.list_datasets(
        security=security,
        query="*:*",
        filters=[],
        start=0,
        rows=10,
    )
    
    assert result == ""


@pytest.mark.asyncio
async def test_list_files_direct(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test list_files with direct file query."""
    files_result = SearchResult(
        documents=[{"id": "file1"}, {"id": "file2"}],
        total=2,
    )
    
    mock_search_engine.query = AsyncMock(return_value=files_result)
    
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.list_files(
        security=security,
        query="*:*",
        filters=[],
        start=0,
        rows=10,
    )
    
    assert "file1" in result
    assert "file2" in result
    assert "data-access-api/download?id=" in result


def test_sanitize_query(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test query sanitization."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    # Test that unsafe characters are rejected
    with pytest.raises(ValueError, match="Unsafe characters"):
        service._sanitize_query("test<value")


def test_sanitize_query_defaults_to_wildcard(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test that empty query defaults to *:*."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    security = SecurityContext(subject="test-user", groups=[])
    
    request = service._build_pass_through_request(
        security=security,
        query="",
        filters=[],
        start=0,
        rows=10,
    )
    
    assert request.query == "*:*"


def test_sanitize_filters(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test filter sanitization."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    # Test that unsafe characters are rejected
    with pytest.raises(ValueError, match="Unsafe characters"):
        service._sanitize_filters(["test<value"])


def test_normalize_rows(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test rows normalization."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    # Test default when rows is 0
    assert service._normalize_rows(0) == 10
    
    # Test default when rows is negative
    assert service._normalize_rows(-5) == 10
    
    # Test that valid rows is returned
    assert service._normalize_rows(50) == 50
    
    # Test that rows is capped at max (min of rows and max_rows)
    assert service._normalize_rows(2000) == min(2000, test_settings.solr_max_rows)


def test_build_or_query(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test OR query building."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    query = service._build_or_query("CollectionId", ["id1", "id2", "id3"])
    assert "CollectionId:" in query
    assert "id1" in query
    assert "id2" in query
    assert "id3" in query
    assert " OR " in query


def test_extract_ids(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test ID extraction from documents."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    docs = [{"id": "id1"}, {"id": "id2"}, {"other": "value"}]
    ids = service._extract_ids(docs)
    
    assert ids == ["id1", "id2"]


def test_build_download_url(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test download URL building."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    url = service._build_download_url("test-file-id")
    assert url == "http://localhost:8000/data-access-api/download?id=test-file-id"
    
    # Test URL encoding
    url = service._build_download_url("test file with spaces")
    assert "test%20file%20with%20spaces" in url


def test_build_urls_from_ids(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test URL list building from IDs."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    
    urls = service._build_urls_from_ids(["id1", "id2", "id3"])
    assert "id1" in urls
    assert "id2" in urls
    assert "id3" in urls
    assert urls.endswith("\n")
    assert urls.count("\n") == 3


def test_build_access_control_filter(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test access control filter building."""
    service = ListService(settings=test_settings, search_engine=mock_search_engine)
    security = SecurityContext(subject="test-user", groups=["group1", "group2"])
    
    filter_str = service._build_access_control_filter(security)
    
    assert filter_str is not None
    assert "OwnerPrincipal:" in filter_str
    assert "group1" in filter_str
    assert "group2" in filter_str
    # Public principal should be included if configured
    if test_settings.public_owner_principal:
        assert test_settings.public_owner_principal in filter_str


def test_build_access_control_filter_super_owner(test_settings: Settings, mock_search_engine: MagicMock) -> None:
    """Test that super owner bypasses access control."""
    # Create a mock settings object to avoid env var overrides
    class MockSettings:
        solr_url = "http://localhost:8983/solr"
        solr_max_rows = 1000
        download_base_url = "http://localhost:8000/data-access-api/download"
        public_owner_principal = "public"
        super_owner_principal = "super-admin"
    
    settings = MockSettings()  # type: ignore
    service = ListService(settings=settings, search_engine=mock_search_engine)  # type: ignore
    security = SecurityContext(subject="test-user", groups=["super-admin"])
    
    filter_str = service._build_access_control_filter(security)
    
    # Super owner should bypass access control - returns None when super_owner is in groups
    assert filter_str is None, f"Expected None but got {filter_str!r}, super_owner={settings.super_owner_principal!r}, groups={security.groups!r}"

