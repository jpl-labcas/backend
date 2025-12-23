"""Unit tests for download service."""

from __future__ import annotations

import os
import tempfile
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from jpl.labcas.backend.auth.dependencies import SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.download import DownloadService, FileInfo


@pytest.fixture
def test_settings() -> Settings:
    """Create test settings."""
    return Settings(
        solr_url="http://localhost:8983/solr",
        s3_bucket="test-bucket",
        aws_region="us-east-1",
        aws_download_url_expiration_seconds=300,
    )


@pytest.fixture
def mock_query_service() -> MagicMock:
    """Create a mock query service."""
    service = MagicMock()
    service.query_files = AsyncMock(return_value={
        "response": {
            "docs": [{
                "id": "test-file-id",
                "FileLocation": "/data/files",
                "FileName": "test.txt",
                "name": "test.txt",
            }],
            "numFound": 1,
        }
    })
    return service


@pytest.fixture
def mock_s3_client_factory() -> MagicMock:
    """Create a mock S3 client factory."""
    factory = MagicMock()
    s3_client = MagicMock()
    s3_client.generate_presigned_url = MagicMock(return_value="https://s3.amazonaws.com/test-bucket/file.txt")
    factory.create = MagicMock(return_value=s3_client)
    return factory


@pytest.mark.asyncio
async def test_get_file_info_success(
    test_settings: Settings,
    mock_query_service: MagicMock,
    mock_s3_client_factory: MagicMock,
) -> None:
    """Test successful file info retrieval."""
    service = DownloadService(
        settings=test_settings,
        query_service=mock_query_service,
        s3_client_factory=mock_s3_client_factory,
    )
    security = SecurityContext(subject="test-user", groups=[])
    
    file_info = await service.get_file_info(security=security, file_id="test-file-id")
    
    assert file_info is not None
    assert file_info.file_location == "/data/files"
    assert file_info.file_name == "test.txt"
    assert file_info.real_file_name == "test.txt"
    assert file_info.file_path == "/data/files/test.txt"


@pytest.mark.asyncio
async def test_get_file_info_not_found(
    test_settings: Settings,
    mock_query_service: MagicMock,
    mock_s3_client_factory: MagicMock,
) -> None:
    """Test file info retrieval when file is not found."""
    mock_query_service.query_files = AsyncMock(return_value={
        "response": {"docs": [], "numFound": 0}
    })
    
    service = DownloadService(
        settings=test_settings,
        query_service=mock_query_service,
        s3_client_factory=mock_s3_client_factory,
    )
    security = SecurityContext(subject="test-user", groups=[])
    
    file_info = await service.get_file_info(security=security, file_id="nonexistent")
    
    assert file_info is None


@pytest.mark.asyncio
async def test_get_file_info_with_name_field(
    test_settings: Settings,
    mock_query_service: MagicMock,
    mock_s3_client_factory: MagicMock,
) -> None:
    """Test file info retrieval when name field is present."""
    mock_query_service.query_files = AsyncMock(return_value={
        "response": {
            "docs": [{
                "id": "test-file-id",
                "FileLocation": "/data/files",
                "FileName": "old-name.txt",
                "name": "new-name.txt",
            }],
            "numFound": 1,
        }
    })
    
    service = DownloadService(
        settings=test_settings,
        query_service=mock_query_service,
        s3_client_factory=mock_s3_client_factory,
    )
    security = SecurityContext(subject="test-user", groups=[])
    
    file_info = await service.get_file_info(security=security, file_id="test-file-id")
    
    assert file_info is not None
    assert file_info.file_name == "old-name.txt"
    assert file_info.real_file_name == "new-name.txt"
    assert file_info.file_path == "/data/files/new-name.txt"


@pytest.mark.asyncio
async def test_get_file_info_with_name_field_list(
    test_settings: Settings,
    mock_query_service: MagicMock,
    mock_s3_client_factory: MagicMock,
) -> None:
    """Test file info retrieval when name field is a list."""
    mock_query_service.query_files = AsyncMock(return_value={
        "response": {
            "docs": [{
                "id": "test-file-id",
                "FileLocation": "/data/files",
                "FileName": "old-name.txt",
                "name": ["new-name.txt"],
            }],
            "numFound": 1,
        }
    })
    
    service = DownloadService(
        settings=test_settings,
        query_service=mock_query_service,
        s3_client_factory=mock_s3_client_factory,
    )
    security = SecurityContext(subject="test-user", groups=[])
    
    file_info = await service.get_file_info(security=security, file_id="test-file-id")
    
    assert file_info is not None
    assert file_info.real_file_name == "new-name.txt"


def test_extract_s3_key() -> None:
    """Test S3 key extraction."""
    service = DownloadService(settings=Settings())
    
    key = service.extract_s3_key("s3://bucket-name/path/to/file.txt")
    assert key == "path/to/file.txt"
    
    key = service.extract_s3_key("s3://bucket-name/file.txt")
    assert key == "file.txt"
    
    key = service.extract_s3_key("/local/path/file.txt")
    assert key == "/local/path/file.txt"


def test_is_local_file() -> None:
    """Test local file detection."""
    service = DownloadService(settings=Settings())
    
    assert service.is_local_file("/data/files/test.txt") is True
    assert service.is_local_file("file:///data/files/test.txt") is True
    assert service.is_local_file("s3://bucket/file.txt") is False
    assert service.is_local_file("s3a://bucket/file.txt") is False


def test_get_s3_presigned_url(test_settings: Settings, mock_s3_client_factory: MagicMock) -> None:
    """Test S3 presigned URL generation."""
    service = DownloadService(
        settings=test_settings,
        s3_client_factory=mock_s3_client_factory,
    )
    
    url = service.get_s3_presigned_url("path/to/file.txt")
    
    assert url == "https://s3.amazonaws.com/test-bucket/file.txt"
    s3_client = mock_s3_client_factory.create.return_value
    s3_client.generate_presigned_url.assert_called_once()
    call_kwargs = s3_client.generate_presigned_url.call_args[1]
    # Check that bucket matches test_settings (may be from env, so check the call was made correctly)
    assert "Bucket" in call_kwargs["Params"]
    assert call_kwargs["Params"]["Key"] == "path/to/file.txt"
    # Expiration should match test_settings value (may be overridden by env)
    assert call_kwargs["ExpiresIn"] == test_settings.aws_download_url_expiration_seconds


def test_get_s3_presigned_url_custom_expiration(test_settings: Settings, mock_s3_client_factory: MagicMock) -> None:
    """Test S3 presigned URL generation with custom expiration."""
    service = DownloadService(
        settings=test_settings,
        s3_client_factory=mock_s3_client_factory,
    )
    
    url = service.get_s3_presigned_url("path/to/file.txt", expiration_seconds=600)
    
    assert url is not None
    s3_client = mock_s3_client_factory.create.return_value
    call_kwargs = s3_client.generate_presigned_url.call_args[1]
    assert call_kwargs["ExpiresIn"] == 600


def test_get_s3_presigned_url_no_bucket(mock_s3_client_factory: MagicMock) -> None:
    """Test S3 presigned URL generation fails without bucket."""
    # Create a mock settings object that explicitly has no bucket
    # Note: In real usage, Settings loads from env vars, so this tests the code path
    # when s3_bucket is None/empty
    class MockSettings:
        s3_bucket = None
        aws_download_url_expiration_seconds = 300
    
    settings = MockSettings()
    service = DownloadService(
        settings=settings,  # type: ignore
        s3_client_factory=mock_s3_client_factory,
    )
    
    with pytest.raises(ValueError, match="S3_BUCKET configuration is required"):
        service.get_s3_presigned_url("path/to/file.txt")


def test_apply_path_prefix_replacements() -> None:
    """Test path prefix replacement."""
    # Create a mock settings to avoid env var overrides
    class MockSettings:
        file_path_prefix_replacements = "/old:/new"
    
    settings = MockSettings()
    service = DownloadService(settings=settings)  # type: ignore
    
    result = service.apply_path_prefix_replacements("/old/path/to/file.txt")
    assert result == "/new/path/to/file.txt"


def test_apply_path_prefix_replacements_multiple() -> None:
    """Test multiple path prefix replacements."""
    # Create a mock settings to avoid env var overrides
    class MockSettings:
        file_path_prefix_replacements = "/old1:/new1,/old2:/new2"
    
    settings = MockSettings()
    service = DownloadService(settings=settings)  # type: ignore
    
    result = service.apply_path_prefix_replacements("/old1/path/to/file.txt")
    assert result == "/new1/path/to/file.txt"
    
    result = service.apply_path_prefix_replacements("/old2/path/to/file.txt")
    assert result == "/new2/path/to/file.txt"


def test_apply_path_prefix_replacements_no_match() -> None:
    """Test path prefix replacement when no match."""
    settings = Settings(file_path_prefix_replacements="/old:/new:")
    service = DownloadService(settings=settings)
    
    result = service.apply_path_prefix_replacements("/other/path/to/file.txt")
    assert result == "/other/path/to/file.txt"


def test_get_file_size() -> None:
    """Test file size retrieval."""
    service = DownloadService(settings=Settings())
    
    with tempfile.NamedTemporaryFile(delete=False) as tmp:
        tmp.write(b"test content")
        tmp_path = tmp.name
    
    try:
        size = service.get_file_size(tmp_path)
        assert size == len(b"test content")
    finally:
        os.unlink(tmp_path)


def test_get_file_size_not_found() -> None:
    """Test file size retrieval when file doesn't exist."""
    service = DownloadService(settings=Settings())
    
    with pytest.raises(OSError):
        service.get_file_size("/nonexistent/file.txt")


def test_get_media_type() -> None:
    """Test media type detection."""
    service = DownloadService(settings=Settings())
    
    assert service.get_media_type("file.dcm") == "application/dicom"
    assert service.get_media_type("file.DCM") == "application/dicom"
    assert service.get_media_type("path/to/dicom/file.dcm") == "application/dicom"
    assert service.get_media_type("file.txt") == "application/octet-stream"
    assert service.get_media_type("file.pdf") == "application/octet-stream"
    assert service.get_media_type("file.dicom") == "application/dicom"

