"""Service for serving non-Solr-cataloged auxiliary assets from disk."""

from __future__ import annotations

import configparser
import logging
import mimetypes
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path, PurePosixPath

from ..auth.dependencies import SecurityContext
from ..config import Settings, get_settings

LOG = logging.getLogger(__name__)

# Explicit MIME overrides for extensions that vary across Python builds / platforms.
_MIME_OVERRIDES: dict[str, str] = {
    ".csv": "text/csv",
    ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ".xls": "application/vnd.ms-excel",
    ".json": "application/json",
    ".js": "text/javascript",
    ".css": "text/css",
    ".svg": "image/svg+xml",
}

SECTION_PREFIX = "file-service "


@dataclass(frozen=True)
class AuxFileServiceConfig:
    """Configuration for a single auxiliary file-service endpoint."""

    key: str
    name: str
    description: str
    filesystem: Path
    groups: list[str]


class AuxFileService:
    """Load auxiliary file-service configs and resolve authorized filesystem paths."""

    def __init__(self, *, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._services: dict[str, AuxFileServiceConfig] | None = None

    @property
    def services(self) -> dict[str, AuxFileServiceConfig]:
        """Lazily load and cache the file-service registry from the INI config."""

        if self._services is None:
            self._services = self._load_services()
        return self._services

    def get_service(self, key: str) -> AuxFileServiceConfig:
        """Return the config for ``key``, or raise ``KeyError`` if unknown."""

        try:
            return self.services[key]
        except KeyError as exc:
            raise KeyError(f"Unknown auxiliary file-service: {key!r}") from exc

    def is_authorized(self, config: AuxFileServiceConfig, security: SecurityContext) -> bool:
        """Return True if ``security`` may access assets under ``config``."""

        if "*" in config.groups:
            return True

        super_owner = (self.settings.super_owner_principal or "").strip()
        if super_owner and super_owner in security.groups:
            return True

        allowed = set(config.groups)
        return bool(allowed.intersection(security.groups))

    def resolve_path(self, config: AuxFileServiceConfig, requested_path: str) -> Path:
        """Resolve ``requested_path`` under ``config.filesystem``, rejecting traversal.

        Raises:
            ValueError: if the path is empty, absolute, or escapes the service root.
        """

        if not requested_path or not requested_path.strip():
            raise ValueError("Requested path must not be empty")

        # Normalize to Posix so URL-style separators are consistent, then reject
        # absolute operands (Path(root) / "/etc/passwd" would discard root).
        posix = PurePosixPath(requested_path)
        if posix.is_absolute():
            raise ValueError(f"Absolute paths are not allowed: {requested_path!r}")

        root = config.filesystem.resolve()
        candidate = (root / Path(*posix.parts)).resolve()

        if not candidate.is_relative_to(root):
            raise ValueError(
                f"Path {requested_path!r} escapes filesystem root {root}"
            )

        return candidate

    def get_media_type(self, file_path: Path) -> str:
        """Guess a MIME type for ``file_path``, falling back to octet-stream."""

        suffix = file_path.suffix.lower()
        if suffix in _MIME_OVERRIDES:
            return _MIME_OVERRIDES[suffix]
        guessed, _ = mimetypes.guess_type(str(file_path))
        return guessed or "application/octet-stream"

    def _load_services(self) -> dict[str, AuxFileServiceConfig]:
        config_path = self.settings.auxfiles_config_path
        if config_path is None:
            LOG.info("No LABCAS_AUXFILES_CONFIG set; auxiliary file services disabled")
            return {}

        path = Path(config_path)
        if not path.is_file():
            LOG.warning("Auxiliary file-services config not found: %s", path)
            return {}

        parser = configparser.ConfigParser()
        parser.read(path, encoding="utf-8")

        services: dict[str, AuxFileServiceConfig] = {}
        for section in parser.sections():
            if not section.startswith(SECTION_PREFIX):
                LOG.warning("Ignoring non-file-service section %r in %s", section, path)
                continue

            key = section[len(SECTION_PREFIX) :].strip()
            if not key:
                LOG.warning("Ignoring empty file-service section name in %s", path)
                continue

            name = parser.get(section, "name", fallback=key).strip()
            if name != key:
                LOG.warning(
                    "file-service section key %r does not match name=%r; using section key",
                    key,
                    name,
                )

            description = parser.get(section, "description", fallback="").strip()
            filesystem_raw = parser.get(section, "filesystem", fallback="").strip()
            if not filesystem_raw:
                LOG.warning("file-service %r missing filesystem; skipping", key)
                continue

            groups_raw = parser.get(section, "groups", fallback="").strip()
            if not groups_raw:
                LOG.warning("file-service %r missing groups; skipping", key)
                continue

            if groups_raw == "*":
                groups = ["*"]
            else:
                groups = [g.strip() for g in groups_raw.split("|") if g.strip()]
                if not groups:
                    LOG.warning("file-service %r has empty groups list; skipping", key)
                    continue

            filesystem = Path(filesystem_raw).expanduser().resolve()
            services[key] = AuxFileServiceConfig(
                key=key,
                name=name,
                description=description,
                filesystem=filesystem,
                groups=groups,
            )
            LOG.info(
                "Loaded auxiliary file-service key=%s filesystem=%s groups=%s",
                key,
                filesystem,
                groups,
            )

        return services


@lru_cache(maxsize=1)
def _cached_aux_file_service() -> AuxFileService:
    return AuxFileService()


def get_aux_file_service() -> AuxFileService:
    """FastAPI dependency hook for the auxiliary file service."""

    return _cached_aux_file_service()
