"""Functional tests for ZIP endpoint."""

from __future__ import annotations

from fastapi.testclient import TestClient

from jpl.labcas.backend.auth.dependencies import SecurityContext, require_authenticated_user
from jpl.labcas.backend.main import create_app
from jpl.labcas.backend.services.zipperlab import get_zipperlab_service


class StubZipperlabService:
    """Stub Zipperlab service for endpoint tests."""

    def __init__(self) -> None:
        self.email: str | None = None
        self.files: list[str] = []
        self.query: str | None = None
        self.ids: list[str] = []
        self.uuid = "zip-request-uuid"

    async def resolve_file_paths(
        self,
        *,
        security: SecurityContext,
        query: str | None,
        ids: list[str],
    ) -> list[str]:
        self.query = query
        self.ids = ids
        return ["/data/files/file1.txt", "/data/files/file2.txt"]

    async def initiate_zip(self, *, email: str, files: list[str]) -> str:
        self.email = email
        self.files = files
        return self.uuid


def _make_app(stub_service: StubZipperlabService) -> TestClient:
    """Create test app with stub service."""

    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(
        subject="test-user",
        groups=["group1"],
    )
    app.dependency_overrides[get_zipperlab_service] = lambda: stub_service
    return TestClient(app)


def test_zip_endpoint_accepts_query_form() -> None:
    """Test /zip accepts email and query form fields."""

    stub_service = StubZipperlabService()
    client = _make_app(stub_service)

    response = client.post(
        "/zip",
        data={
            "email": "hello@example.org",
            "query": "id:FILE*",
        },
    )

    assert response.status_code == 200
    assert response.text == "zip-request-uuid"
    assert stub_service.email == "hello@example.org"
    assert stub_service.query == "id:FILE*"
    assert stub_service.ids == []
    assert stub_service.files == ["/data/files/file1.txt", "/data/files/file2.txt"]


def test_zip_endpoint_accepts_repeated_id_form_values() -> None:
    """Test /zip accepts repeated id form fields."""

    stub_service = StubZipperlabService()
    client = _make_app(stub_service)

    response = client.post(
        "/zip",
        data={
            "email": "hello@example.org",
            "id": ["FILE1", "FILE2"],
        },
    )

    assert response.status_code == 200
    assert response.text == "zip-request-uuid"
    assert stub_service.ids == ["FILE1", "FILE2"]
    assert stub_service.query is None


def test_zip_endpoint_requires_authentication() -> None:
    """Test /zip requires authentication."""

    app = create_app()
    client = TestClient(app)

    response = client.post(
        "/zip",
        data={
            "email": "hello@example.org",
            "query": "id:FILE*",
        },
    )

    assert response.status_code == 401

