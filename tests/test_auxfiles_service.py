"""Unit tests for the auxiliary file-service module."""

from __future__ import annotations

from pathlib import Path

import pytest

from jpl.labcas.backend.auth.dependencies import SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.auxfiles import AuxFileService, AuxFileServiceConfig


SUPER_OWNER = "cn=Super User,dc=edrn,dc=jpl,dc=nasa,dc=gov"
GROUP_A = "cn=Crichton JPL,dc=edrn,dc=jpl,dc=nasa,dc=gov"
GROUP_B = "cn=Amos Geisel School of Medicine at Dartmouth,dc=edrn,dc=jpl,dc=nasa,dc=gov"


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

[other-section]
ignored = yes
""".strip()
        + "\n",
        encoding="utf-8",
    )


def _settings(*, config_path: Path | None, super_owner: str | None = SUPER_OWNER) -> Settings:
    kwargs: dict = {
        "LABCAS_DIRECTORY_PROVIDER": "mock",
        "LABCAS_SUPER_OWNER_PRINCIPAL": super_owner,
    }
    if config_path is not None:
        kwargs["LABCAS_AUXFILES_CONFIG"] = str(config_path)
    return Settings(**kwargs)


def test_load_services_parses_ini(tmp_path: Path) -> None:
    public_root = tmp_path / "public"
    private_root = tmp_path / "private"
    public_root.mkdir()
    private_root.mkdir()
    ini = tmp_path / "file-services.ini"
    _write_ini(ini, public_root=public_root, private_root=private_root)

    service = AuxFileService(settings=_settings(config_path=ini))
    services = service.services

    assert set(services) == {"open-to-public", "minerva"}
    assert services["open-to-public"].groups == ["*"]
    assert services["open-to-public"].filesystem == public_root.resolve()
    assert services["minerva"].groups == [GROUP_A, GROUP_B]
    assert services["minerva"].filesystem == private_root.resolve()


def test_load_services_empty_when_unset() -> None:
    service = AuxFileService(settings=_settings(config_path=None))
    assert service.services == {}


def test_get_service_unknown_raises(tmp_path: Path) -> None:
    public_root = tmp_path / "public"
    private_root = tmp_path / "private"
    public_root.mkdir()
    private_root.mkdir()
    ini = tmp_path / "file-services.ini"
    _write_ini(ini, public_root=public_root, private_root=private_root)

    service = AuxFileService(settings=_settings(config_path=ini))
    with pytest.raises(KeyError, match="Unknown"):
        service.get_service("does-not-exist")


def test_is_authorized_public() -> None:
    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="open-to-public",
        name="open-to-public",
        description="",
        filesystem=Path("/tmp"),
        groups=["*"],
    )
    guest = SecurityContext(subject="uid=guest,ou=public", groups=[])
    assert service.is_authorized(config, guest) is True


def test_is_authorized_group_match() -> None:
    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="minerva",
        name="minerva",
        description="",
        filesystem=Path("/tmp"),
        groups=[GROUP_A, GROUP_B],
    )
    member = SecurityContext(subject="uid=alice,dc=example", groups=[GROUP_B])
    outsider = SecurityContext(subject="uid=bob,dc=example", groups=["cn=Other,dc=example"])
    guest = SecurityContext(subject="uid=guest,ou=public", groups=[])

    assert service.is_authorized(config, member) is True
    assert service.is_authorized(config, outsider) is False
    assert service.is_authorized(config, guest) is False


def test_is_authorized_super_owner_bypass() -> None:
    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="minerva",
        name="minerva",
        description="",
        filesystem=Path("/tmp"),
        groups=[GROUP_A],
    )
    super_user = SecurityContext(subject="uid=admin,dc=example", groups=[SUPER_OWNER])
    assert service.is_authorized(config, super_user) is True


def test_resolve_path_happy(tmp_path: Path) -> None:
    root = tmp_path / "assets"
    nested = root / "something" / "or" / "other"
    nested.mkdir(parents=True)
    target = nested / "123.json"
    target.write_text('{"ok": true}', encoding="utf-8")

    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="minerva",
        name="minerva",
        description="",
        filesystem=root,
        groups=["*"],
    )

    resolved = service.resolve_path(config, "something/or/other/123.json")
    assert resolved == target.resolve()
    assert resolved.is_file()


def test_resolve_path_rejects_empty(tmp_path: Path) -> None:
    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="x",
        name="x",
        description="",
        filesystem=tmp_path,
        groups=["*"],
    )
    with pytest.raises(ValueError, match="empty"):
        service.resolve_path(config, "")
    with pytest.raises(ValueError, match="empty"):
        service.resolve_path(config, "   ")


def test_resolve_path_rejects_traversal(tmp_path: Path) -> None:
    root = tmp_path / "assets"
    root.mkdir()
    (root / "ok.txt").write_text("ok", encoding="utf-8")
    outside = tmp_path / "secret.txt"
    outside.write_text("secret", encoding="utf-8")

    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="x",
        name="x",
        description="",
        filesystem=root,
        groups=["*"],
    )

    with pytest.raises(ValueError, match="escapes"):
        service.resolve_path(config, "../../../secret.txt")
    with pytest.raises(ValueError, match="escapes"):
        service.resolve_path(config, "ok.txt/../../secret.txt")


def test_resolve_path_rejects_absolute(tmp_path: Path) -> None:
    root = tmp_path / "assets"
    root.mkdir()
    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="x",
        name="x",
        description="",
        filesystem=root,
        groups=["*"],
    )

    with pytest.raises(ValueError, match="Absolute"):
        service.resolve_path(config, "/etc/shadow")
    with pytest.raises(ValueError, match="Absolute"):
        service.resolve_path(config, "//etc/shadow")


def test_resolve_path_rejects_symlink_escape(tmp_path: Path) -> None:
    root = tmp_path / "assets"
    root.mkdir()
    outside = tmp_path / "outside.txt"
    outside.write_text("leaked", encoding="utf-8")
    link = root / "escape.txt"
    link.symlink_to(outside)

    service = AuxFileService(settings=_settings(config_path=None))
    config = AuxFileServiceConfig(
        key="x",
        name="x",
        description="",
        filesystem=root,
        groups=["*"],
    )

    with pytest.raises(ValueError, match="escapes"):
        service.resolve_path(config, "escape.txt")


def test_get_media_type_overrides() -> None:
    service = AuxFileService(settings=_settings(config_path=None))
    assert service.get_media_type(Path("data.csv")) == "text/csv"
    assert service.get_media_type(Path("sheet.xlsx")).startswith("application/")
    assert service.get_media_type(Path("data.json")) == "application/json"
    assert service.get_media_type(Path("unknown.zzz")) == "application/octet-stream"
