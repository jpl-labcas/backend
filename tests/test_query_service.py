"""Unit tests for query service."""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
import httpx

from jpl.labcas.backend.auth.dependencies import SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.query import QueryService


@pytest.fixture
def test_settings() -> Settings:
    """Create test settings."""
    return Settings(
        solr_url="http://localhost:8983/solr",
        solr_max_rows=1000,
        solr_verify_ssl=True,
        public_owner_principal="public",
        super_owner_principal=None,
    )


@pytest.fixture
def mock_httpx_client() -> AsyncMock:
    """Create a mock httpx client."""
    client = AsyncMock(spec=httpx.AsyncClient)
    response = MagicMock()
    response.json.return_value = {"response": {"docs": [], "numFound": 0}}
    response.raise_for_status = MagicMock()
    client.get = AsyncMock(return_value=response)
    return client


@pytest.mark.asyncio
async def test_query_collections(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test querying collections core."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.query_collections(security=security, params={"q": "*:*"})
    
    assert result is not None
    assert "response" in result
    assert mock_httpx_client.get.called
    call_args = mock_httpx_client.get.call_args
    assert call_args[0][0] == "/collections/select"


@pytest.mark.asyncio
async def test_query_datasets(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test querying datasets core."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.query_datasets(security=security, params={"q": "*:*"})
    
    assert result is not None
    assert "response" in result
    assert mock_httpx_client.get.called
    call_args = mock_httpx_client.get.call_args
    assert call_args[0][0] == "/datasets/select"


@pytest.mark.asyncio
async def test_query_files(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test querying files core."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.query_files(security=security, params={"q": "*:*"})
    
    assert result is not None
    assert "response" in result
    assert mock_httpx_client.get.called
    call_args = mock_httpx_client.get.call_args
    assert call_args[0][0] == "/files/select"


@pytest.mark.asyncio
async def test_build_access_control_filter_with_groups(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that access control filter includes user groups."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1", "group2"])
    
    await service.query_collections(security=security, params={"q": "*:*"})
    
    call_args = mock_httpx_client.get.call_args
    params = call_args[1]["params"]
    assert "fq" in params
    fq = params["fq"]
    assert isinstance(fq, list)
    assert any("group1" in f for f in fq)
    assert any("group2" in f for f in fq)


@pytest.mark.asyncio
async def test_build_access_control_filter_with_public_principal(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that access control filter includes public principal."""
    # Create settings with public_owner_principal set
    class MockSettings:
        solr_url = "http://localhost:8983/solr"
        solr_max_rows = 1000
        solr_verify_ssl = True
        public_owner_principal = "public"
        super_owner_principal = None
    
    settings = MockSettings()  # type: ignore
    service = QueryService(settings=settings, client=mock_httpx_client)  # type: ignore
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    await service.query_collections(security=security, params={"q": "*:*"})
    
    call_args = mock_httpx_client.get.call_args
    params = call_args[1]["params"]
    assert "fq" in params
    fq = params["fq"]
    assert isinstance(fq, list)
    assert any("public" in f for f in fq)


@pytest.mark.asyncio
async def test_build_access_control_filter_super_owner(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that super owner bypasses access control."""
    # Create settings with super_owner_principal set
    class MockSettings:
        solr_url = "http://localhost:8983/solr"
        solr_max_rows = 1000
        solr_verify_ssl = True
        public_owner_principal = "public"
        super_owner_principal = "super-admin"
    
    settings = MockSettings()  # type: ignore
    service = QueryService(settings=settings, client=mock_httpx_client)  # type: ignore
    security = SecurityContext(subject="test-user", groups=["super-admin"])
    
    await service.query_collections(security=security, params={"q": "*:*"})
    
    call_args = mock_httpx_client.get.call_args
    params = call_args[1]["params"]
    # Super owner should not have access control filter (returns None, so no OwnerPrincipal filter)
    if "fq" in params:
        fq = params["fq"]
        # If fq exists, it might be from other params, but OwnerPrincipal filter should not be added
        owner_filters = [f for f in fq if "OwnerPrincipal" in str(f)]
        assert len(owner_filters) == 0, f"Super owner should bypass access control, but found: {owner_filters}"


@pytest.mark.asyncio
async def test_sanitize_params_removes_unsafe_chars(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that parameters are sanitized."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=[])
    
    with pytest.raises(ValueError, match="Unsafe characters"):
        await service.query_collections(security=security, params={"q": "test<value"})


@pytest.mark.asyncio
async def test_rows_limit_enforcement(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that rows parameter is limited."""
    # Create settings with lower max_rows for testing
    class MockSettings:
        solr_url = "http://localhost:8983/solr"
        solr_max_rows = 1000
        solr_verify_ssl = True
        public_owner_principal = None
        super_owner_principal = None
    
    settings = MockSettings()  # type: ignore
    service = QueryService(settings=settings, client=mock_httpx_client)  # type: ignore
    security = SecurityContext(subject="test-user", groups=[])
    
    with pytest.raises(ValueError, match="rows must be ≤"):
        await service.query_collections(security=security, params={"rows": 2000})


@pytest.mark.asyncio
async def test_rows_limit_allows_max(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that max rows value is allowed."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=[])
    
    result = await service.query_collections(security=security, params={"rows": 1000})
    
    assert result is not None
    call_args = mock_httpx_client.get.call_args
    params = call_args[1]["params"]
    assert params["rows"] == 1000


@pytest.mark.asyncio
async def test_wt_parameter_defaults_to_json(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that wt parameter defaults to json."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=[])
    
    await service.query_collections(security=security, params={"q": "*:*"})
    
    call_args = mock_httpx_client.get.call_args
    params = call_args[1]["params"]
    assert params.get("wt") == "json"


@pytest.mark.asyncio
async def test_multi_value_fq_parameters(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that multiple fq parameters are handled correctly."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    await service.query_collections(security=security, params={"q": "*:*", "fq": ["field1:value1", "field2:value2"]})
    
    call_args = mock_httpx_client.get.call_args
    params = call_args[1]["params"]
    assert "fq" in params
    fq = params["fq"]
    assert isinstance(fq, list)
    assert len(fq) >= 2  # Should include access control filter plus the two provided


def test_query_service_requires_solr_url() -> None:
    """Test that QueryService requires SOLR_URL."""
    # Create a mock settings object since Settings loads from env
    class MockSettings:
        solr_url = None
        solr_verify_ssl = True
    
    settings = MockSettings()  # type: ignore
    
    with pytest.raises(ValueError, match="SOLR_URL configuration is required"):
        QueryService(settings=settings)  # type: ignore

