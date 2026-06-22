"""Unit tests for shared access control filter construction."""

from __future__ import annotations

from jpl.labcas.backend.auth.dependencies import GUEST_USER_DN, SecurityContext
from jpl.labcas.backend.config import Settings
from jpl.labcas.backend.services.access_control import NO_ACCESS_FILTER, build_owner_principal_filter


def test_build_owner_principal_filter_with_groups() -> None:
    settings = Settings(
        solr_url="http://localhost:8983/solr",
        public_owner_principal="public",
        super_owner_principal=None,
    )
    security = SecurityContext(subject="uid=tester,ou=users,dc=example,dc=com", groups=["group1", "group2"])

    filter_str = build_owner_principal_filter(settings, security)

    assert filter_str is not None
    assert filter_str != NO_ACCESS_FILTER
    assert "OwnerPrincipal:" in filter_str
    assert "group1" in filter_str
    assert "group2" in filter_str
    assert "public" in filter_str


def test_build_owner_principal_filter_super_owner() -> None:
    settings = Settings(
        solr_url="http://localhost:8983/solr",
        public_owner_principal="public",
        super_owner_principal="super-admin",
    )
    security = SecurityContext(subject="uid=tester,ou=users,dc=example,dc=com", groups=["super-admin"])

    assert build_owner_principal_filter(settings, security) is None


def test_build_owner_principal_filter_authenticated_without_groups() -> None:
    settings = Settings(
        solr_url="http://localhost:8983/solr",
        public_owner_principal="public",
        super_owner_principal=None,
    )
    security = SecurityContext(subject="uid=tester,ou=users,dc=example,dc=com", groups=[])

    assert build_owner_principal_filter(settings, security) == NO_ACCESS_FILTER


def test_build_owner_principal_filter_guest_without_groups() -> None:
    settings = Settings(
        solr_url="http://localhost:8983/solr",
        public_owner_principal="public",
        super_owner_principal=None,
    )
    security = SecurityContext(subject=GUEST_USER_DN, groups=[])

    filter_str = build_owner_principal_filter(settings, security)

    assert filter_str is not None
    assert filter_str != NO_ACCESS_FILTER
    assert "public" in filter_str
