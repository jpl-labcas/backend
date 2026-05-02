"""Unit tests for Zipperlab service."""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from jpl.labcas.backend.auth.dependencies import SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.zipperlab import ZipperlabService


@pytest.fixture
def test_settings() -> Settings:
    """Create test settings."""

    return Settings(
        solr_url="http://localhost:8983/solr",
        solr_max_rows=1000,
        zipperlab_url="http://localhost:6468/edrn/",
        public_owner_principal="public",
    )


@pytest.fixture
def mock_query_service() -> MagicMock:
    """Create a mock query service."""

    service = MagicMock()
    service.query_files = AsyncMock(
        return_value={
            "response": {
                "docs": [
                    {
                        "id": "file1",
                        "FileLocation": "/data/files",
                        "FileName": "old-name.txt",
                        "name": "new-name.txt",
                    },
                    {
                        "id": "file2",
                        "FileLocation": "/data/files",
                        "FileName": "second.txt",
                    },
                ],
                "numFound": 2,
            }
        }
    )
    return service


def test_default_client_allows_self_signed_certificates(test_settings: Settings) -> None:
    """Test the default Zipperlab client skips certificate verification."""

    with patch("jpl.labcas.backend.services.zipperlab.httpx.AsyncClient") as client_class:
        client = MagicMock()
        client_class.return_value = client

        service = ZipperlabService(settings=test_settings)

    assert service.client is client
    client_class.assert_called_once_with(verify=False)


@pytest.mark.asyncio
async def test_resolve_query_file_paths(test_settings: Settings, mock_query_service: MagicMock) -> None:
    """Test resolving file paths from a Solr query."""

    service = ZipperlabService(settings=test_settings, query_service=mock_query_service, client=AsyncMock())
    security = SecurityContext(subject="test-user", groups=["group1"])

    paths = await service.resolve_file_paths(security=security, query="id:FILE*", ids=[])

    assert paths == ["/data/files/new-name.txt", "/data/files/second.txt"]
    call_args = mock_query_service.query_files.call_args
    assert call_args.kwargs["security"] == security
    assert call_args.kwargs["params"]["q"] == "id:FILE*"
    assert call_args.kwargs["params"]["fl"] == "FileLocation,FileName,name"


@pytest.mark.asyncio
async def test_resolve_query_file_paths_decodes_frontend_encoded_query(
    test_settings: Settings,
    mock_query_service: MagicMock,
) -> None:
    """Test /zip query values tolerate Java-compatible double URL encoding."""

    service = ZipperlabService(settings=test_settings, query_service=mock_query_service, client=AsyncMock())
    security = SecurityContext(subject="test-user", groups=["group1"])

    await service.resolve_file_paths(
        security=security,
        query="id:(Pre-diagnostic_PDAC_Images/UPMC/Case%201/CT%201/file.dcm)",
        ids=[],
    )

    params = mock_query_service.query_files.call_args.kwargs["params"]
    assert params["q"] == "id:(Pre-diagnostic_PDAC_Images/UPMC/Case 1/CT 1/file.dcm)"


@pytest.mark.asyncio
async def test_resolve_id_file_paths(test_settings: Settings, mock_query_service: MagicMock) -> None:
    """Test resolving file paths from repeated file IDs."""

    service = ZipperlabService(settings=test_settings, query_service=mock_query_service, client=AsyncMock())
    security = SecurityContext(subject="test-user", groups=[])

    paths = await service.resolve_file_paths(security=security, query=None, ids=["FILE1", "FILE2"])

    assert paths == ["/data/files/new-name.txt", "/data/files/second.txt"]
    params = mock_query_service.query_files.call_args.kwargs["params"]
    assert params["q"] == 'id:("FILE1" OR "FILE2")'
    assert params["rows"] == 2


@pytest.mark.asyncio
async def test_resolve_id_file_paths_decodes_frontend_encoded_ids(
    test_settings: Settings,
    mock_query_service: MagicMock,
) -> None:
    """Test repeated /zip id values match the Java double-decode workaround."""

    service = ZipperlabService(settings=test_settings, query_service=mock_query_service, client=AsyncMock())
    security = SecurityContext(subject="test-user", groups=[])

    await service.resolve_file_paths(
        security=security,
        query=None,
        ids=[
            "Pre-diagnostic_PDAC_Images/UPMC/Case%201/CT%201/file.dcm",
            "Pre-diagnostic_PDAC_Images/UPMC/Case+2/CT+2/file.dcm",
        ],
    )

    params = mock_query_service.query_files.call_args.kwargs["params"]
    assert params["q"] == (
        'id:("Pre-diagnostic_PDAC_Images/UPMC/Case 1/CT 1/file.dcm" OR '
        '"Pre-diagnostic_PDAC_Images/UPMC/Case 2/CT 2/file.dcm")'
    )


@pytest.mark.asyncio
async def test_initiate_zip_posts_java_compatible_payload(test_settings: Settings) -> None:
    """Test initiating a ZIP request."""

    client = AsyncMock(spec=httpx.AsyncClient)
    response = MagicMock()
    response.text = "zip-request-uuid\n"
    response.raise_for_status = MagicMock()
    client.post = AsyncMock(return_value=response)
    service = ZipperlabService(settings=test_settings, client=client)

    uuid = await service.initiate_zip(
        email="hello@example.org",
        files=["/data/files/file1.txt", "/data/files/file2.txt"],
    )

    assert uuid == "zip-request-uuid"
    client.post.assert_awaited_once_with(
        "http://localhost:6468/edrn/",
        json={
            "operation": "initiate",
            "email": "hello@example.org",
            "files": ["/data/files/file1.txt", "/data/files/file2.txt"],
        },
    )
    response.raise_for_status.assert_called_once()


@pytest.mark.asyncio
async def test_resolve_file_paths_rejects_ambiguous_request(test_settings: Settings) -> None:
    """Test that callers must choose query or IDs."""

    service = ZipperlabService(settings=test_settings, client=AsyncMock())
    security = SecurityContext(subject="test-user", groups=[])

    with pytest.raises(ValueError, match="either query or id"):
        await service.resolve_file_paths(security=security, query="*:*", ids=["FILE1"])

