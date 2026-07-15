"""LDAP directory provider implementation."""

from __future__ import annotations

import logging
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Iterator

from ldap3 import Connection, NONE, Server
from ldap3.core.exceptions import LDAPBindError, LDAPException, LDAPSocketReceiveError

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

        self._use_ssl = self.settings.ldap_uri.startswith("ldaps")

    def _make_server(self) -> Server:
        """Return an isolated server handle for a single LDAP connection."""
        return Server(self.settings.ldap_uri, get_info=NONE, use_ssl=self._use_ssl)

    def _constructed_user_dn(self, username: str) -> str | None:
        if not self.settings.ldap_user_base:
            return None
        return f"uid={username},{self.settings.ldap_user_base}"

    def _try_bind(self, user_dn: str, password: str) -> bool:
        """Attempt a user bind on a dedicated connection; return True when credentials are accepted."""
        conn = Connection(
            self._make_server(),
            user=user_dn,
            password=password,
            auto_bind=False,
            check_names=False,
        )
        try:
            if conn.bind(read_server_info=False):
                return True
        except LDAPBindError:
            return False
        except LDAPSocketReceiveError:
            if conn.bound:
                return True
            return False
        finally:
            try:
                conn.unbind()
            except LDAPException:
                pass

        return False

    def authenticate(self, username: str, password: str) -> DirectoryUser | None:
        """Authenticate a user and return a directory user."""
        if not username or not password:
            return None

        constructed_dn = self._constructed_user_dn(username)
        if constructed_dn and self._try_bind(constructed_dn, password):
            return DirectoryUser(username=username, dn=constructed_dn)

        user_dn = self._resolve_user_dn(username)
        if not user_dn:
            return None

        if self._try_bind(user_dn, password):
            return DirectoryUser(username=username, dn=user_dn)

        LOG.debug("LDAP bind failed for user %s (invalid credentials)", username)
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
        except Exception:  # noqa: BLE001
            LOG.exception("Failed to resolve DN for user %s", username)
        return None

    @contextmanager
    def _admin_connection(self) -> Iterator[Connection]:
        """Yield a connection bound with admin credentials."""

        if not self.settings.ldap_bind_dn or not self.settings.ldap_password:
            msg = "Admin bind credentials are required for LDAP operations."
            raise ValueError(msg)

        conn = Connection(
            self._make_server(),
            user=self.settings.ldap_bind_dn,
            password=self.settings.ldap_password,
            auto_bind=False,
            check_names=False,
        )
        try:
            if not conn.bind(read_server_info=False):
                msg = "Admin LDAP bind failed."
                raise RuntimeError(msg)
            yield conn
        finally:
            try:
                conn.unbind()
            except LDAPException:
                pass

    @staticmethod
    def _parse_timestamp(value: str) -> datetime:
        try:
            # LDAP timestamps are typically in the format YYYYmmddHHMMSSZ
            return datetime.strptime(value, "%Y%m%d%H%M%SZ").replace(tzinfo=timezone.utc)
        except ValueError:
            LOG.warning("Unparseable LDAP timestamp %s", value)
            return epoch()

