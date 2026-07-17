"""Shared Solr access-control filter construction."""

from __future__ import annotations

import logging

from ..auth.dependencies import SecurityContext
from ..config import Settings

_logger = logging.getLogger(__name__)

# Solr filter query that matches no documents when combined with any positive query.
NO_ACCESS_FILTER = "-*:*"


def build_owner_principal_filter(settings: Settings, security: SecurityContext) -> str | None:
    """Build an OwnerPrincipal filter for Solr, or None to bypass access control.

    Users with no LDAP groups still receive the public principal (when configured) so they
    can browse metadata. Downloads are gated separately via :func:`user_may_download`.
    """

    _logger.info("🔒 Building owner principal filter for security context: %r", security)

    _logger.info("🦸 Super owner principal from settings is: %r", settings.super_owner_principal)
    super_owner = (settings.super_owner_principal or "").strip()
    if super_owner and super_owner in security.groups:
        _logger.info("🦸 Super owner principal found in security context, returning None")
        return None

    principals: list[str] = []
    if settings.public_owner_principal:
        principals.append(settings.public_owner_principal.strip())
    principals.extend(security.groups)
    principals = [principal for principal in principals if principal]

    if not principals:
        _logger.info("👎 No principals found, returning NO_ACCESS_FILTER")
        return NO_ACCESS_FILTER

    unique = list(dict.fromkeys(principals))
    joined = " OR ".join(f'"{principal}"' for principal in unique)
    rc = f"OwnerPrincipal:({joined})"
    _logger.info("🔒 Owner principal filter built: %r", rc)
    return rc


def user_may_download(security: SecurityContext) -> bool:
    """Return True when the user is allowed to download files.

    Authenticated users must belong to at least one LDAP group. Metadata and query access
    are not gated by this check.
    """

    return bool(security.groups)
