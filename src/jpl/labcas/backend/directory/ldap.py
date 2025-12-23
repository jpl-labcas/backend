"""LDAP directory provider implementation."""

from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Iterable

from ldap3 import Connection, ALL, NTLM, Server, Tls

from ..config import Settings, get_settings
from .base import DirectoryProvider, DirectoryUser, epoch

LOG = logging.getLogger(__name__)


class LdapDirectoryProvider(DirectoryProvider):
    """Directory provider backed by an LDAP server."""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()

        if not self.settings.ldap_uri:
            msg = "LDAP_URI configuration is required for the LDAP directory provider."
            raise ValueError(msg)

        self._server = Server(self.settings.ldap_uri, get_info=ALL, use_ssl=self.settings.ldap_uri.startswith("ldaps"))

    def authenticate(self, username: str, password: str) -> DirectoryUser | None:
        """Authenticate a user and return a directory user."""
        if not username or not password:
            return None

        user_dn = self._resolve_user_dn(username)
        if not user_dn:
            return None

        LOG.debug("Attempting LDAP bind for %s", user_dn)
        try:
            with Connection(self._server, user=user_dn, password=password, auto_bind=True) as conn:
                if conn.bound:
                    return DirectoryUser(username=username, dn=user_dn)
        except Exception:  # noqa: BLE001
            LOG.warning("LDAP bind failed for user %s", username, exc_info=True)
            return None

        return None

    def get_groups(self, user: DirectoryUser) -> list[str]:
        """Return the groups associated with the user."""

        if not self.settings.ldap_group_base:
            return []

        groups: list[str] = []
        search_filter = f"(uniqueMember={user.dn})"
        try:
            with self._admin_connection() as conn:
                conn.search(
                    search_base=self.settings.ldap_group_base,
                    search_filter=search_filter,
                    attributes=["cn"],
                )
                groups = [entry.entry_dn for entry in conn.entries]
        except Exception:  # noqa: BLE001
            LOG.warning("Failed to retrieve LDAP groups for %s", user.dn, exc_info=True)

        return groups

    def get_last_modified(self, user: DirectoryUser) -> datetime:
        """Return the last modification timestamp for the user."""

        if not self.settings.ldap_user_base:
            return epoch()

        try:
            with self._admin_connection() as conn:
                conn.search(
                    search_base=user.dn,
                    search_filter="(objectClass=*)",
                    attributes=["modifyTimestamp"],
                )

                if not conn.entries:
                    return epoch()

                timestamp = conn.entries[0]["modifyTimestamp"]
                if timestamp:
                    return self._parse_timestamp(str(timestamp))
        except Exception:  # noqa: BLE001
            LOG.warning("Failed to read modifyTimestamp for %s", user.dn, exc_info=True)

        return epoch()

    # Internal helpers -------------------------------------------------

    def _resolve_user_dn(self, username: str) -> str | None:
        if not self.settings.ldap_user_base:
            return None

        search_filter = f"(uid={username})"
        try:
            with self._admin_connection() as conn:
                conn.search(
                    search_base=self.settings.ldap_user_base,
                    search_filter=search_filter,
                    attributes=[],  # Only need the DN, which is available via entry_dn
                )
                if conn.entries:
                    return conn.entries[0].entry_dn
        except Exception as e:  # noqa: BLE001
            LOG.exception("Failed to resolve DN for user %s", username, exc_info=True)
        return None

    def _admin_connection(self) -> Connection:
        """Return a connection bound with admin credentials."""

        if not self.settings.ldap_bind_dn or not self.settings.ldap_password:
            msg = "Admin bind credentials are required for LDAP operations."
            raise ValueError(msg)

        return Connection(
            self._server,
            user=self.settings.ldap_bind_dn,
            password=self.settings.ldap_password,
            auto_bind=True,
        )

    @staticmethod
    def _parse_timestamp(value: str) -> datetime:
        try:
            # LDAP timestamps are typically in the format YYYYmmddHHMMSSZ
            return datetime.strptime(value, "%Y%m%d%H%M%SZ").replace(tzinfo=timezone.utc)
        except ValueError:
            LOG.warning("Unparseable LDAP timestamp %s", value)
            return epoch()


