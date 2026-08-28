"""Unit tests for query service."""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
import httpx

from jpl.labcas.backend.auth.dependencies import GUEST_USER_DN, SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.access_control import NO_ACCESS_FILTER
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
    client.post = AsyncMock(return_value=response)
    return client


@pytest.mark.asyncio
async def test_query_collections(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test querying collections core."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.query_collections(security=security, params={"q": "*:*"})
    
    assert result is not None
    assert "response" in result
    assert mock_httpx_client.post.called
    call_args = mock_httpx_client.post.call_args
    assert call_args[0][0] == "/collections/select"


@pytest.mark.asyncio
async def test_query_datasets(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test querying datasets core."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.query_datasets(security=security, params={"q": "*:*"})
    
    assert result is not None
    assert "response" in result
    assert mock_httpx_client.post.called
    call_args = mock_httpx_client.post.call_args
    assert call_args[0][0] == "/datasets/select"


@pytest.mark.asyncio
async def test_query_files(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test querying files core."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    result = await service.query_files(security=security, params={"q": "*:*"})
    
    assert result is not None
    assert "response" in result
    assert mock_httpx_client.post.called
    call_args = mock_httpx_client.post.call_args
    assert call_args[0][0] == "/files/select"


@pytest.mark.asyncio
async def test_build_access_control_filter_with_groups(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that access control filter includes user groups."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1", "group2"])
    
    await service.query_collections(security=security, params={"q": "*:*"})
    
    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
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
    
    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
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
    
    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
    # Super owner should not have access control filter (returns None, so no OwnerPrincipal filter)
    if "fq" in params:
        fq = params["fq"]
        # If fq exists, it might be from other params, but OwnerPrincipal filter should not be added
        owner_filters = [f for f in fq if "OwnerPrincipal" in str(f)]
        assert len(owner_filters) == 0, f"Super owner should bypass access control, but found: {owner_filters}"


@pytest.mark.asyncio
async def test_build_access_control_filter_authenticated_without_groups(
    test_settings: Settings,
    mock_httpx_client: AsyncMock,
) -> None:
    """Authenticated users with no LDAP groups may still query public metadata."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="uid=tester,ou=users,dc=example,dc=com", groups=[])

    await service.query_collections(security=security, params={"q": "*:*"})

    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
    assert "fq" in params
    fq = params["fq"]
    assert isinstance(fq, list)
    assert any("public" in str(filter_query) for filter_query in fq)
    assert not any(NO_ACCESS_FILTER in str(filter_query) for filter_query in fq)


@pytest.mark.asyncio
async def test_build_access_control_filter_guest_still_gets_public(
    test_settings: Settings,
    mock_httpx_client: AsyncMock,
) -> None:
    """Guest users should still be limited to the public principal."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject=GUEST_USER_DN, groups=[])

    await service.query_collections(security=security, params={"q": "*:*"})

    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
    assert "fq" in params
    fq = params["fq"]
    assert isinstance(fq, list)
    assert any("public" in str(filter_query) for filter_query in fq)
    assert not any(NO_ACCESS_FILTER in str(filter_query) for filter_query in fq)


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
    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
    assert params["rows"] == 1000


@pytest.mark.asyncio
async def test_wt_parameter_defaults_to_json(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that wt parameter defaults to json."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=[])
    
    await service.query_collections(security=security, params={"q": "*:*"})
    
    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
    assert params.get("wt") == "json"


@pytest.mark.asyncio
async def test_multi_value_fq_parameters(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    """Test that multiple fq parameters are handled correctly."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])
    
    await service.query_collections(security=security, params={"q": "*:*", "fq": ["field1:value1", "field2:value2"]})
    
    call_args = mock_httpx_client.post.call_args
    params = call_args[1]["data"]
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


def _solr_json_response(docs: list[dict]) -> MagicMock:
    response = MagicMock()
    response.json.return_value = {"response": {"docs": docs, "numFound": len(docs), "start": 0}}
    response.raise_for_status = MagicMock()
    return response


def _count_or_clauses(query: str) -> int:
    parsed = QueryService._parse_or_group_query(query)
    if parsed is not None:
        return len(parsed[1])
    open_paren = query.find("(")
    if open_paren != -1 and query.endswith(")"):
        body = query[open_paren + 1 : -1].strip()
        return 1 if body else 0
    return 0


def test_split_or_clauses_ignores_quoted_delimiter() -> None:
    clauses = QueryService._split_or_clauses('"a OR b" OR "c" OR "d\\" OR e"')
    assert clauses == ['"a OR b"', '"c"', '"d\\" OR e"']


def test_parse_or_group_query_extracts_id_clauses() -> None:
    parsed = QueryService._parse_or_group_query('id:("FILE1" OR "FILE2")')
    assert parsed == ("id:(", ['"FILE1"', '"FILE2"'], ")")


@pytest.mark.asyncio
async def test_short_or_query_is_not_batched(test_settings: Settings, mock_httpx_client: AsyncMock) -> None:
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    security = SecurityContext(subject="test-user", groups=["group1"])

    await service.query_files(security=security, params={"q": 'id:("FILE1" OR "FILE2")', "rows": 2})

    assert mock_httpx_client.post.call_count == 1
    params = mock_httpx_client.post.call_args.kwargs["data"]
    assert params["q"] == 'id:("FILE1" OR "FILE2")'


@pytest.mark.asyncio
async def test_query_core_batches_long_or_query_and_merges_docs(
    test_settings: Settings,
    mock_httpx_client: AsyncMock,
) -> None:
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    service.MAX_SOLR_BOOLEAN_CLAUSES = 3
    security = SecurityContext(subject="test-user", groups=["group1"])
    file_ids = [f"FILE{index}" for index in range(7)]
    query = "id:(" + " OR ".join(f'"{file_id}"' for file_id in file_ids) + ")"

    mock_httpx_client.post.side_effect = [
        _solr_json_response([{"id": "FILE0"}, {"id": "FILE1"}, {"id": "FILE2"}]),
        _solr_json_response([{"id": "FILE3"}, {"id": "FILE4"}, {"id": "FILE5"}]),
        _solr_json_response([{"id": "FILE6"}]),
    ]

    result = await service.query_files(security=security, params={"q": query, "rows": 3, "fl": "id"})

    assert mock_httpx_client.post.call_count == 3
    posted_queries = [call.kwargs["data"]["q"] for call in mock_httpx_client.post.call_args_list]
    assert [_count_or_clauses(query_text) for query_text in posted_queries] == [3, 3, 1]
    assert all(call.kwargs["data"]["start"] == 0 for call in mock_httpx_client.post.call_args_list)
    assert [call.kwargs["data"]["rows"] for call in mock_httpx_client.post.call_args_list] == [3, 3, 3]
    assert [doc["id"] for doc in result["response"]["docs"]] == file_ids
    assert result["response"]["numFound"] == 7


@pytest.mark.asyncio
async def test_batched_query_returns_all_docs_even_when_rows_is_capped(
    test_settings: Settings,
    mock_httpx_client: AsyncMock,
) -> None:
    """/zip caps rows at solr_max_rows; batched ID lookups must still return every match."""
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    service.MAX_SOLR_BOOLEAN_CLAUSES = 2
    security = SecurityContext(subject="test-user", groups=["group1"])
    file_ids = [f"FILE{index}" for index in range(5)]
    query = "id:(" + " OR ".join(f'"{file_id}"' for file_id in file_ids) + ")"

    mock_httpx_client.post.side_effect = [
        _solr_json_response([{"id": "FILE0"}, {"id": "FILE1"}]),
        _solr_json_response([{"id": "FILE2"}, {"id": "FILE3"}]),
        _solr_json_response([{"id": "FILE4"}]),
    ]

    result = await service.query_files(security=security, params={"q": query, "rows": 2})

    assert mock_httpx_client.post.call_count == 3
    assert [doc["id"] for doc in result["response"]["docs"]] == file_ids
    assert result["response"]["numFound"] == 5


@pytest.mark.asyncio
async def test_query_core_batches_by_character_length(
    test_settings: Settings,
    mock_httpx_client: AsyncMock,
) -> None:
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    service.MAX_SOLR_QUERY_CHARS = 32
    security = SecurityContext(subject="test-user", groups=["group1"])
    query = 'id:("aaaaaaaaaa" OR "bbbbbbbbbb" OR "cccccccccc")'

    mock_httpx_client.post.side_effect = [
        _solr_json_response([{"id": "aaaaaaaaaa"}]),
        _solr_json_response([{"id": "bbbbbbbbbb"}]),
        _solr_json_response([{"id": "cccccccccc"}]),
    ]

    result = await service.query_files(security=security, params={"q": query, "rows": 3})

    assert mock_httpx_client.post.call_count == 3
    assert all(len(call.kwargs["data"]["q"]) <= 32 for call in mock_httpx_client.post.call_args_list)
    assert result["response"]["numFound"] == 3


@pytest.mark.asyncio
async def test_unsplittable_query_is_sent_in_one_request(
    test_settings: Settings,
    mock_httpx_client: AsyncMock,
) -> None:
    service = QueryService(settings=test_settings, client=mock_httpx_client)
    service.MAX_SOLR_QUERY_CHARS = 10
    security = SecurityContext(subject="test-user", groups=["group1"])

    await service.query_files(security=security, params={"q": "collection:very-long-name-without-or-clauses"})

    assert mock_httpx_client.post.call_count == 1
    params = mock_httpx_client.post.call_args.kwargs["data"]
    assert params["q"] == "collection:very-long-name-without-or-clauses"

