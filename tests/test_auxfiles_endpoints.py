"""Functional tests for the /auxfiles endpoint."""

from __future__ import annotations

from pathlib import Path

from fastapi.testclient import TestClient

from jpl.labcas.backend.auth.dependencies import SecurityContext, get_security_context
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.main import create_app
from jpl.labcas.backend.services.auxfiles import AuxFileService, get_aux_file_service


GROUP_A = "cn=Crichton JPL,dc=edrn,dc=jpl,dc=nasa,dc=gov"
GROUP_B = "cn=Amos Geisel School of Medicine at Dartmouth,dc=edrn,dc=jpl,dc=nasa,dc=gov"
SUPER_OWNER = "cn=Super User,dc=edrn,dc=jpl,dc=nasa,dc=gov"


def _write_ini(path: Path, *, public_root: Path, private_root: Path) -> None:
    path.write_text(
        f"""
[file-service open-to-public]
name = open-to-public
description = Public files
filesystem = {public_root}
groups = *

[file-service minerva]
name = minerva
description = Private Minerva assets
filesystem = {private_root}
groups = {GROUP_A}|{GROUP_B}
""".strip()
        + "\n",
        encoding="utf-8",
    )


def _make_client(
    tmp_path: Path,
    *,
    security: SecurityContext | None = None,
) -> tuple[TestClient, Path, Path]:
    public_root = tmp_path / "public"
    private_root = tmp_path / "private"
    public_root.mkdir()
    private_root.mkdir()
    (public_root / "metadata.xlsx").write_bytes(b"PK\x03\x04public")
    nested = private_root / "something" / "or" / "other" / "123"
    nested.mkdir(parents=True)
    (nested / "4561193.json").write_text('{"secret": true}', encoding="utf-8")

    ini = tmp_path / "file-services.ini"
    _write_ini(ini, public_root=public_root, private_root=private_root)

    settings = Settings(
        LABCAS_DIRECTORY_PROVIDER="mock",
        LABCAS_AUXFILES_CONFIG=str(ini),
        LABCAS_SUPER_OWNER_PRINCIPAL=SUPER_OWNER,
        LABCAS_SUBPATH_PREFIX="",
    )
    aux_service = AuxFileService(settings=settings)

    app = create_app(settings=settings)
    app.dependency_overrides[get_aux_file_service] = lambda: aux_service
    app.dependency_overrides[get_security_context] = lambda: security or SecurityContext(
        subject="uid=guest,ou=public",
        groups=[],
    )
    return TestClient(app), public_root, private_root


def test_auxfiles_public_anonymous_ok(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    response = client.get("/auxfiles/open-to-public/metadata.xlsx")

    assert response.status_code == 200
    assert response.content == b"PK\x03\x04public"
    assert "spreadsheetml" in response.headers["content-type"]
    assert "content-disposition" not in response.headers


def test_auxfiles_download_query_sets_content_disposition(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    response = client.get("/auxfiles/open-to-public/metadata.xlsx", params={"download": "true"})

    assert response.status_code == 200
    assert 'attachment; filename="metadata.xlsx"' in response.headers["content-disposition"]


def test_auxfiles_head_returns_headers_only(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    response = client.head("/auxfiles/open-to-public/metadata.xlsx")

    assert response.status_code == 200
    assert response.content == b""
    assert response.headers["content-length"] == str(len(b"PK\x03\x04public"))


def test_auxfiles_private_denied_for_guest(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    response = client.get("/auxfiles/minerva/something/or/other/123/4561193.json")

    assert response.status_code == 403


def test_auxfiles_private_ok_for_group_member(tmp_path: Path) -> None:
    client, _, _ = _make_client(
        tmp_path,
        security=SecurityContext(subject="uid=alice,dc=example", groups=[GROUP_A]),
    )

    response = client.get("/auxfiles/minerva/something/or/other/123/4561193.json")

    assert response.status_code == 200
    assert response.json() == {"secret": True}
    assert response.headers["content-type"].startswith("application/json")


def test_auxfiles_private_ok_for_super_owner(tmp_path: Path) -> None:
    client, _, _ = _make_client(
        tmp_path,
        security=SecurityContext(subject="uid=admin,dc=example", groups=[SUPER_OWNER]),
    )

    response = client.get("/auxfiles/minerva/something/or/other/123/4561193.json")

    assert response.status_code == 200
    assert response.json() == {"secret": True}


def test_auxfiles_unknown_service_404(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    response = client.get("/auxfiles/no-such-service/file.txt")

    assert response.status_code == 404


def test_auxfiles_missing_file_404(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    response = client.get("/auxfiles/open-to-public/does-not-exist.txt")

    assert response.status_code == 404


def test_auxfiles_directory_listing_blocked(tmp_path: Path) -> None:
    client, public_root, _ = _make_client(tmp_path)
    (public_root / "subdir").mkdir()

    response = client.get("/auxfiles/open-to-public/subdir")

    assert response.status_code == 404


def test_auxfiles_path_traversal_denied(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    # Percent-encode ".." so the TestClient does not normalize the URL before routing.
    response = client.get("/auxfiles/open-to-public/%2e%2e/%2e%2e/%2e%2e/etc/shadow")

    assert response.status_code == 400


def test_auxfiles_relative_traversal_denied(tmp_path: Path) -> None:
    client, public_root, _ = _make_client(tmp_path)
    (public_root / "subdir").mkdir()
    outside = tmp_path / "outside.txt"
    outside.write_text("nope", encoding="utf-8")

    response = client.get("/auxfiles/open-to-public/subdir/%2e%2e/%2e%2e/outside.txt")

    assert response.status_code == 400


def test_auxfiles_absolute_path_denied(tmp_path: Path) -> None:
    client, _, _ = _make_client(tmp_path)

    # Starlette may normalize leading slashes in path params; still ensure rejection.
    response = client.get("/auxfiles/open-to-public/%2Fetc%2Fshadow")

    assert response.status_code in (400, 404)


def test_auxfiles_symlink_escape_denied(tmp_path: Path) -> None:
    client, public_root, _ = _make_client(tmp_path)
    outside = tmp_path / "outside-secret.txt"
    outside.write_text("leaked", encoding="utf-8")
    (public_root / "escape.txt").symlink_to(outside)

    response = client.get("/auxfiles/open-to-public/escape.txt")

    assert response.status_code == 400


def test_auxfiles_disabled_when_no_config(tmp_path: Path) -> None:
    settings = Settings(
        LABCAS_DIRECTORY_PROVIDER="mock",
        LABCAS_SUBPATH_PREFIX="",
    )
    aux_service = AuxFileService(settings=settings)
    app = create_app(settings=settings)
    app.dependency_overrides[get_aux_file_service] = lambda: aux_service
    app.dependency_overrides[get_security_context] = lambda: SecurityContext(
        subject="uid=guest,ou=public",
        groups=[],
    )
    client = TestClient(app)

    response = client.get("/auxfiles/open-to-public/metadata.xlsx")

    assert response.status_code == 404
