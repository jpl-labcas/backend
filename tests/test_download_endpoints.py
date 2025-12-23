"""Functional tests for download endpoint."""

from __future__ import annotations

import os
import tempfile
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock

import pytest
from fastapi.testclient import TestClient

from jpl.labcas.backend.auth.dependencies import SecurityContext, require_authenticated_user
from jpl.labcas.backend.main import create_app
from jpl.labcas.backend.services.download import DownloadService, FileInfo, get_download_service
from jpl.labcas.backend.services.query import QueryService


class StubDownloadService:
    """Stub download service for testing."""

    def __init__(self) -> None:
        self.file_info: FileInfo | None = None
        self.file_id: str | None = None
        self.is_local: bool = True
        self.s3_key: str | None = None
        self.presigned_url: str = "https://s3.amazonaws.com/bucket/file.txt"

    async def get_file_info(self, *, security: SecurityContext, file_id: str) -> FileInfo | None:
        self.file_id = file_id
        return self.file_info

    def is_local_file(self, file_location: str) -> bool:
        return self.is_local

    def extract_s3_key(self, file_path: str) -> str:
        return self.s3_key or "path/to/file.txt"

    def get_s3_presigned_url(self, s3_key: str, expiration_seconds: int | None = None) -> str:
        return self.presigned_url

    def apply_path_prefix_replacements(self, file_path: str) -> str:
        return file_path

    def get_file_size(self, file_path: str) -> int:
        return 1024

    def get_media_type(self, file_path: str) -> str:
        return "application/octet-stream"


def _make_app(stub_service: StubDownloadService) -> TestClient:
    """Create test app with stub service."""
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="test-user", groups=["group1"]
    )
    app.dependency_overrides[get_download_service] = lambda: stub_service
    return TestClient(app)


def test_download_local_file() -> None:
    """Test /data-access-api/download with local file."""
    stub_service = StubDownloadService()
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".txt") as tmp:
        tmp.write("test content")
        tmp_path = tmp.name

    try:
        stub_service.file_info = FileInfo(
            file_location=os.path.dirname(tmp_path),
            file_name=os.path.basename(tmp_path),
            real_file_name=os.path.basename(tmp_path),
            file_path=tmp_path,
        )
        stub_service.is_local = True

        client = _make_app(stub_service)

        response = client.get(
            "/data-access-api/download",
            params={"id": "test-file-id"},
        )

        assert response.status_code == 200
        assert response.content == b"test content"
        assert stub_service.file_id == "test-file-id"
    finally:
        os.unlink(tmp_path)


def test_download_local_file_with_content_disposition() -> None:
    """Test /data-access-api/download includes Content-Disposition header."""
    stub_service = StubDownloadService()
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".txt") as tmp:
        tmp.write("test content")
        tmp_path = tmp.name

    try:
        stub_service.file_info = FileInfo(
            file_location=os.path.dirname(tmp_path),
            file_name="test-file.txt",
            real_file_name="test-file.txt",
            file_path=tmp_path,
        )
        stub_service.is_local = True

        client = _make_app(stub_service)

        response = client.get(
            "/data-access-api/download",
            params={"id": "test-file-id"},
        )

        assert response.status_code == 200
        assert "Content-Disposition" in response.headers
        assert "attachment" in response.headers["Content-Disposition"]
        assert "test-file.txt" in response.headers["Content-Disposition"]
    finally:
        os.unlink(tmp_path)


def test_download_suppress_content_disposition() -> None:
    """Test /data-access-api/download can suppress Content-Disposition."""
    stub_service = StubDownloadService()
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".txt") as tmp:
        tmp.write("test content")
        tmp_path = tmp.name

    try:
        stub_service.file_info = FileInfo(
            file_location=os.path.dirname(tmp_path),
            file_name="test-file.txt",
            real_file_name="test-file.txt",
            file_path=tmp_path,
        )
        stub_service.is_local = True

        client = _make_app(stub_service)

        response = client.get(
            "/data-access-api/download",
            params={"id": "test-file-id", "suppressContentDisposition": "true"},
        )

        assert response.status_code == 200
        assert "Content-Disposition" not in response.headers
    finally:
        os.unlink(tmp_path)


def test_download_s3_file_redirects() -> None:
    """Test /data-access-api/download redirects to S3 for S3 files."""
    stub_service = StubDownloadService()
    stub_service.file_info = FileInfo(
        file_location="s3://bucket",
        file_name="file.txt",
        real_file_name="file.txt",
        file_path="s3://bucket/path/to/file.txt",
    )
    stub_service.is_local = False
    stub_service.s3_key = "path/to/file.txt"
    stub_service.presigned_url = "https://s3.amazonaws.com/bucket/path/to/file.txt?signature=xyz"

    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/download",
        params={"id": "test-file-id"},
        follow_redirects=False,
    )

    assert response.status_code == 307
    assert response.headers["location"] == stub_service.presigned_url


def test_download_file_not_found() -> None:
    """Test /data-access-api/download returns 404 when file not found."""
    stub_service = StubDownloadService()
    stub_service.file_info = None

    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/download",
        params={"id": "nonexistent-file-id"},
    )

    assert response.status_code == 404
    assert "not found" in response.json()["detail"].lower()


def test_download_requires_authentication() -> None:
    """Test /data-access-api/download requires authentication."""
    app = create_app()
    client = TestClient(app)

    response = client.get(
        "/data-access-api/download",
        params={"id": "test-file-id"},
    )

    assert response.status_code == 401


def test_download_rejects_unsafe_characters() -> None:
    """Test /data-access-api/download rejects unsafe characters in ID."""
    stub_service = StubDownloadService()
    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/download",
        params={"id": "test<file"},
    )

    assert response.status_code == 400
    assert "unsafe characters" in response.json()["detail"].lower()


def test_download_file_not_found_on_disk() -> None:
    """Test /data-access-api/download returns 404 when file doesn't exist on disk."""
    stub_service = StubDownloadService()
    stub_service.file_info = FileInfo(
        file_location="/nonexistent",
        file_name="file.txt",
        real_file_name="file.txt",
        file_path="/nonexistent/file.txt",
    )
    stub_service.is_local = True

    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/download",
        params={"id": "test-file-id"},
    )

    assert response.status_code == 404
    assert "not found" in response.json()["detail"].lower()


def test_download_with_dicom_file() -> None:
    """Test /data-access-api/download handles DICOM files correctly."""
    stub_service = StubDownloadService()
    with tempfile.NamedTemporaryFile(mode="wb", delete=False, suffix=".dcm") as tmp:
        tmp.write(b"dicom content")
        tmp_path = tmp.name

    try:
        stub_service.file_info = FileInfo(
            file_location=os.path.dirname(tmp_path),
            file_name=os.path.basename(tmp_path),
            real_file_name=os.path.basename(tmp_path),
            file_path=tmp_path,
        )
        stub_service.is_local = True

        # Mock get_media_type to return DICOM type
        original_get_media_type = stub_service.get_media_type

        def get_dicom_media_type(file_path: str) -> str:
            if file_path.endswith(".dcm"):
                return "application/dicom"
            return original_get_media_type(file_path)

        stub_service.get_media_type = get_dicom_media_type

        client = _make_app(stub_service)

        response = client.get(
            "/data-access-api/download",
            params={"id": "test-file-id"},
        )

        assert response.status_code == 200
        assert response.headers["content-type"] == "application/dicom"
    finally:
        os.unlink(tmp_path)


def test_download_with_quotes_in_filename() -> None:
    """Test /data-access-api/download handles quotes in filename."""
    stub_service = StubDownloadService()
    with tempfile.NamedTemporaryFile(mode="w", delete=False, suffix=".txt") as tmp:
        tmp.write("test content")
        tmp_path = tmp.name

    try:
        # Create a filename with quotes (simulated)
        stub_service.file_info = FileInfo(
            file_location=os.path.dirname(tmp_path),
            file_name='test"file".txt',
            real_file_name='test"file".txt',
            file_path=tmp_path,
        )
        stub_service.is_local = True

        client = _make_app(stub_service)

        response = client.get(
            "/data-access-api/download",
            params={"id": "test-file-id"},
        )

        assert response.status_code == 200
        # Content-Disposition should escape quotes
        assert "Content-Disposition" in response.headers
        assert '\\"' in response.headers["Content-Disposition"] or '"' in response.headers["Content-Disposition"]
    finally:
        os.unlink(tmp_path)

