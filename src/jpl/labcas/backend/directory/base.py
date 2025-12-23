"""Base classes for directory providers."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Protocol, runtime_checkable


@dataclass(slots=True)
class DirectoryUser:
    """Representation of an authenticated directory user."""

    username: str
    dn: str

    def __post_init__(self) -> None:
        if not self.dn:
            msg = "Distinguished name must not be empty."
            raise ValueError(msg)


@runtime_checkable
class DirectoryProvider(Protocol):
    """Protocol for directory services."""

    def authenticate(self, username: str, password: str) -> DirectoryUser | None:
        """Validate credentials and return a directory user."""

    def get_groups(self, user: DirectoryUser) -> list[str]:
        """Return the security groups for the given user."""

    def get_last_modified(self, user: DirectoryUser) -> datetime:
        """Return the directory record modification time for the user."""


def epoch() -> datetime:
    """Return the Unix epoch."""

    return datetime.fromtimestamp(0, tz=timezone.utc)


