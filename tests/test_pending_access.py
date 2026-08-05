"""Integration tests for pending-account access control."""

from __future__ import annotations

import base64
from unittest.mock import MagicMock

from fastapi.testclient import TestClient

from jpl.labcas.backend.auth.dependencies import (
    GUEST_USER_DN,
    get_directory_provider,
    get_jwt_manager,
)
from jpl.labcas.backend.auth.jwt_manager import JwtManager
from jpl.labcas.backend.directory import MockDirectoryProvider
from jpl.labcas.backend.main import create_app
from jpl.labcas.backend.services.download import get_download_service
from jpl.labcas.backend.services.listing import get_list_service
from jpl.labcas.backend.services.query import get_query_service


PENDING_DN = "uid=pendinguser,ou=users,dc=example,dc=com"
APPROVED_DN = "uid=approveduser,ou=users,dc=example,dc=com"


class StubQueryService:
    def __init__(self) -> None:
        self.last_security = None

    async def query_collections(self, *, security, params):  # noqa: ANN001
        self.last_security = security
        return {"response": {"docs": [], "numFound": 0, "start": 0}}

    async def query_datasets(self, *, security, params):  # noqa: ANN001
        self.last_security = security
        return {"response": {"docs": [], "numFound": 0, "start": 0}}

    async def query_files(self, *, security, params):  # noqa: ANN001
        self.last_security = security
        return {"response": {"docs": [], "numFound": 0, "start": 0}}


class StubListService:
    async def list_collections(self, **kwargs) -> str:  # noqa: ANN003
        return "ok\n"

    async def list_datasets(self, **kwargs) -> str:  # noqa: ANN003
        return "ok\n"

    async def list_files(self, **kwargs) -> str:  # noqa: ANN003
        return "ok\n"


class StubDownloadService:
    async def get_file_info(self, **kwargs):  # noqa: ANN003
        return None

    def create_aspera_transfer_request(self, **kwargs):  # noqa: ANN003
        return {"ok": True}


def _pending_directory() -> MockDirectoryProvider:
    directory = MockDirectoryProvider()
    directory.add_user("pendinguser", "pendingpass", PENDING_DN, pending=True)
    directory.add_user("approveduser", "approvedpass", APPROVED_DN, pending=False)
    directory.set_groups(APPROVED_DN, ["group1"])
    return directory


def test_pending_jwt_falls_back_to_guest_on_files_select() -> None:
    """Pending JWTs browse file metadata with guest-equivalent access."""
    directory = _pending_directory()
    jwt_manager = MagicMock(spec=JwtManager)
    jwt_manager.verify_token.return_value = {"sub": PENDING_DN}
    stub = StubQueryService()

    app = create_app()
    app.dependency_overrides[get_directory_provider] = lambda: directory
    app.dependency_overrides[get_jwt_manager] = lambda: jwt_manager
    app.dependency_overrides[get_query_service] = lambda: stub
    client = TestClient(app)

    response = client.get(
        "/files/select",
        params={"q": "*:*"},
        headers={"Authorization": "Bearer pending-token"},
    )

    assert response.status_code == 200
    assert stub.last_security is not None
    assert stub.last_security.subject == GUEST_USER_DN


def test_pending_basic_auth_rejected_on_download() -> None:
    directory = _pending_directory()
    app = create_app()
    app.dependency_overrides[get_directory_provider] = lambda: directory
    app.dependency_overrides[get_download_service] = lambda: StubDownloadService()
    client = TestClient(app)

    credentials = base64.b64encode(b"pendinguser:pendingpass").decode("utf-8")
    response = client.get(
        "/download",
        params={"id": "file-1"},
        headers={"Authorization": f"Basic {credentials}"},
    )

    assert response.status_code == 401
    assert "pending" in response.json()["detail"].lower()


def test_pending_jwt_falls_back_to_guest_on_collections_select() -> None:
    """Stale pending cookies should not elevate browse access above guest."""
    directory = _pending_directory()
    jwt_manager = MagicMock(spec=JwtManager)
    jwt_manager.verify_token.return_value = {"sub": PENDING_DN}
    stub = StubQueryService()

    app = create_app()
    app.dependency_overrides[get_directory_provider] = lambda: directory
    app.dependency_overrides[get_jwt_manager] = lambda: jwt_manager
    app.dependency_overrides[get_query_service] = lambda: stub
    client = TestClient(app)

    response = client.get(
        "/collections/select",
        params={"q": "*:*"},
        headers={"Authorization": "Bearer pending-token"},
    )

    assert response.status_code == 200
    assert stub.last_security is not None
    assert stub.last_security.subject == GUEST_USER_DN


def test_guest_can_browse_collections_datasets_and_files() -> None:
    stub = StubQueryService()
    app = create_app()
    app.dependency_overrides[get_query_service] = lambda: stub
    client = TestClient(app)

    collections = client.get("/collections/select", params={"q": "*:*"})
    datasets = client.get("/datasets/select", params={"q": "*:*"})
    files = client.get("/files/select", params={"q": "*:*"})

    assert collections.status_code == 200
    assert datasets.status_code == 200
    assert files.status_code == 200
    assert stub.last_security is not None
    assert stub.last_security.subject == GUEST_USER_DN


def test_pending_rejected_on_list_and_aspera() -> None:
    directory = _pending_directory()
    jwt_manager = MagicMock(spec=JwtManager)
    jwt_manager.verify_token.return_value = {"sub": PENDING_DN}

    app = create_app()
    app.dependency_overrides[get_directory_provider] = lambda: directory
    app.dependency_overrides[get_jwt_manager] = lambda: jwt_manager
    app.dependency_overrides[get_list_service] = lambda: StubListService()
    app.dependency_overrides[get_download_service] = lambda: StubDownloadService()
    client = TestClient(app)
    headers = {"Authorization": "Bearer pending-token"}

    assert client.get("/collections/list", params={"q": "*:*"}, headers=headers).status_code == 401
    assert client.get("/datasets/list", params={"q": "*:*"}, headers=headers).status_code == 401
    assert client.get("/files/list", params={"q": "*:*"}, headers=headers).status_code == 401
    assert (
        client.get(
            "/rapidly-download-collection",
            params={"collectionID": "c1", "token": "t"},
            headers=headers,
        ).status_code
        == 401
    )
