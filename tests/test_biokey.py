"""Unit tests for LDAP biokey parsing and pending-account status."""

from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from jpl.labcas.backend.directory.base import DirectoryUser
from jpl.labcas.backend.directory.biokey import is_pending, parse_biokey
from jpl.labcas.backend.directory.ldap import LdapDirectoryProvider
from jpl.labcas.backend.directory.mock import MockDirectoryProvider


@pytest.mark.parametrize(
    ("description", "expected"),
    [
        (None, None),
        ("", None),
        ("no marker here", None),
        ('@@biokey={"pending":"false"}', {"pending": "false"}),
        ('prefix @@biokey={"pending":"true","role":"user"} suffix', {"pending": "true", "role": "user"}),
        ("@@biokey=not-json", None),
        ('@@biokey=["not","an","object"]', None),
        ("@@biokey=", None),
    ],
)
def test_parse_biokey(description: str | None, expected: dict | None) -> None:
    assert parse_biokey(description) == expected


@pytest.mark.parametrize(
    ("biokey", "expected_pending"),
    [
        (None, False),
        ({}, False),
        ({"pending": "true"}, True),
        ({"pending": "True"}, False),
        ({"pending": "FALSE"}, False),
        ({"pending": False}, False),
        ({"pending": True}, False),
        ({"pending": "false"}, False),
        ({"pending": "false", "other": "x"}, False),
        ({"pending": "true", "other": "x"}, True),
    ],
)
def test_is_pending(biokey: dict | None, expected_pending: bool) -> None:
    assert is_pending(biokey) is expected_pending


def test_mock_directory_pending_defaults_approved() -> None:
    directory = MockDirectoryProvider()
    directory.add_user("alice", "secret")
    user = directory.authenticate("alice", "secret")
    assert user is not None
    assert directory.is_pending(user) is False


def test_mock_directory_set_pending() -> None:
    directory = MockDirectoryProvider()
    directory.add_user("bob", "secret", pending=True)
    user = directory.authenticate("bob", "secret")
    assert user is not None
    assert directory.is_pending(user) is True

    directory.set_pending(user.dn, False)
    assert directory.is_pending(user) is False


def test_ldap_is_pending_approved() -> None:
    settings = MagicMock()
    settings.ldap_uri = "ldaps://ldap.example.com"
    settings.ldap_bind_dn = "cn=admin"
    settings.ldap_password = "secret"
    provider = LdapDirectoryProvider(settings=settings)
    user = DirectoryUser(username="alice", dn="uid=alice,ou=users,dc=example,dc=com")

    with patch.object(provider, "_read_description", return_value='@@biokey={"pending":"false"}'):
        assert provider.is_pending(user) is False


def test_ldap_is_pending_when_missing_biokey() -> None:
    settings = MagicMock()
    settings.ldap_uri = "ldaps://ldap.example.com"
    settings.ldap_bind_dn = "cn=admin"
    settings.ldap_password = "secret"
    provider = LdapDirectoryProvider(settings=settings)
    user = DirectoryUser(username="alice", dn="uid=alice,ou=users,dc=example,dc=com")

    with patch.object(provider, "_read_description", return_value="no biokey"):
        assert provider.is_pending(user) is False


def test_ldap_is_pending_fail_closed_on_error() -> None:
    settings = MagicMock()
    settings.ldap_uri = "ldaps://ldap.example.com"
    settings.ldap_bind_dn = "cn=admin"
    settings.ldap_password = "secret"
    provider = LdapDirectoryProvider(settings=settings)
    user = DirectoryUser(username="alice", dn="uid=alice,ou=users,dc=example,dc=com")

    with patch.object(provider, "_read_description", side_effect=RuntimeError("ldap down")):
        assert provider.is_pending(user) is True
