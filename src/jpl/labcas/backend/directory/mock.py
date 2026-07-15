"""Mock directory provider for testing and development."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Dict, List

from .base import DirectoryProvider, DirectoryUser, epoch


class MockDirectoryProvider(DirectoryProvider):
    """In-memory directory provider useful for tests."""

    def __init__(self, users: Dict[str, str] | None = None, groups: Dict[str, List[str]] | None = None) -> None:
        self._users = users or {"guest": "guest"}
        self._groups = groups or {}
        self._modified: Dict[str, datetime] = {}

    def authenticate(self, username: str, password: str) -> DirectoryUser | None:
        stored_password = self._users.get(username)
        if stored_password is None or stored_password != password:
            return None
        return DirectoryUser(username=username, dn=f"uid={username},ou=users,dc=example,dc=com")

    def get_groups(self, user: DirectoryUser) -> List[str]:
        return self._groups.get(user.dn, [])

    def get_last_modified(self, user: DirectoryUser) -> datetime:
        return self._modified.get(user.dn, epoch())

    # Utilities for tests ------------------------------------------------

    def add_user(self, username: str, password: str, dn: str | None = None) -> None:
        self._users[username] = password
        dn = dn or f"uid={username},ou=users,dc=example,dc=com"
        self._groups.setdefault(dn, [])
        self._modified[dn] = datetime.now(tz=timezone.utc)

    def set_groups(self, dn: str, groups: List[str]) -> None:
        self._groups[dn] = groups
        self._modified[dn] = datetime.now(tz=timezone.utc)


