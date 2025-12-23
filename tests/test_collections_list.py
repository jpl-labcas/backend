"""Tests for the /collections/list endpoint."""

from __future__ import annotations

from fastapi.testclient import TestClient

from jpl.labcas.backend.auth.dependencies import SecurityContext, require_authenticated_user
from jpl.labcas.backend.main import create_app
from jpl.labcas.backend.services.listing import get_list_service


class StubListService:
    """Minimal stub that captures invocation details."""

    def __init__(self) -> None:
        self.collections_payload: dict[str, object] | None = None
        self.datasets_payload: dict[str, object] | None = None
        self.files_payload: dict[str, object] | None = None

    async def list_collections(self, **kwargs) -> str:  # type: ignore[override]
        self.collections_payload = kwargs
        return "http://example.test/data-access-api/download?id=collection-file\n"

    async def list_datasets(self, **kwargs) -> str:  # type: ignore[override]
        self.datasets_payload = kwargs
        return "http://example.test/data-access-api/download?id=dataset-file\n"

    async def list_files(self, **kwargs) -> str:  # type: ignore[override]
        self.files_payload = kwargs
        return "http://example.test/data-access-api/download?id=file\n"


def _make_app(stub_service: StubListService) -> TestClient:
    app = create_app()
    app.dependency_overrides[require_authenticated_user] = lambda: SecurityContext(subject="tester", groups=["grp"])
    app.dependency_overrides[get_list_service] = lambda: stub_service
    return TestClient(app)


def test_collections_list_returns_plain_text_response() -> None:
    stub_service = StubListService()
    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/collections/list",
        params=[
            ("q", "Field:value"),
            ("fq", "OwnerPrincipal:\"grp\""),
            ("rows", "5"),
            ("start", "0"),
        ],
    )

    assert response.status_code == 200
    assert response.text == "http://example.test/data-access-api/download?id=collection-file\n"
    assert response.headers["content-type"].startswith("text/plain")
    assert stub_service.collections_payload is not None
    assert stub_service.collections_payload["query"] == "Field:value"
    assert stub_service.collections_payload["filters"] == ["OwnerPrincipal:\"grp\""]
    assert stub_service.collections_payload["rows"] == 5
    assert stub_service.collections_payload["start"] == 0


def test_datasets_list_returns_plain_text_response() -> None:
    stub_service = StubListService()
    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/datasets/list",
        params=[
            ("q", "Dataset:value"),
            ("fq", "OwnerPrincipal:\"grp\""),
            ("rows", "3"),
            ("start", "2"),
        ],
    )

    assert response.status_code == 200
    assert response.text == "http://example.test/data-access-api/download?id=dataset-file\n"
    assert response.headers["content-type"].startswith("text/plain")
    assert stub_service.datasets_payload is not None
    assert stub_service.datasets_payload["query"] == "Dataset:value"
    assert stub_service.datasets_payload["filters"] == ["OwnerPrincipal:\"grp\""]
    assert stub_service.datasets_payload["rows"] == 3
    assert stub_service.datasets_payload["start"] == 2


def test_files_list_returns_plain_text_response() -> None:
    stub_service = StubListService()
    client = _make_app(stub_service)

    response = client.get(
        "/data-access-api/files/list",
        params=[
            ("q", "File:value"),
            ("fq", "OwnerPrincipal:\"grp\""),
            ("rows", "7"),
            ("start", "1"),
        ],
    )

    assert response.status_code == 200
    assert response.text == "http://example.test/data-access-api/download?id=file\n"
    assert response.headers["content-type"].startswith("text/plain")
    assert stub_service.files_payload is not None
    assert stub_service.files_payload["query"] == "File:value"
    assert stub_service.files_payload["filters"] == ["OwnerPrincipal:\"grp\""]
    assert stub_service.files_payload["rows"] == 7
    assert stub_service.files_payload["start"] == 1

