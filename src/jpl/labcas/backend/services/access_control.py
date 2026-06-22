"""Shared Solr access-control filter construction."""

from __future__ import annotations

from ..auth.dependencies import GUEST_USER_DN, SecurityContext
from ..config import Settings
import logging

_logger = logging.getLogger(__name__)

# Solr filter query that matches no documents when combined with any positive query.
NO_ACCESS_FILTER = "-*:*"


def build_owner_principal_filter(settings: Settings, security: SecurityContext) -> str | None:
    """Build an OwnerPrincipal filter for Solr, or None to bypass access control."""

    _logger.info('🔒 Building owner principal filter for security context: %r', security)

    super_owner = (settings.super_owner_principal or "").strip()
    if super_owner and super_owner in security.groups:
        _logger.info('🦸 Super owner principal found in security context, returning None')
        return None

    is_guest = security.subject == GUEST_USER_DN
    if not is_guest and not security.groups:
        _logger.info('🚫 No groups found in security context, returning NO_ACCESS_FILTER')
        return NO_ACCESS_FILTER

    principals: list[str] = []
    if settings.public_owner_principal:
        principals.append(settings.public_owner_principal.strip())
    principals.extend(security.groups)
    principals = [principal for principal in principals if principal]

    if not principals:
        _logger.info('👎 No principals found, returning NO_ACCESS_FILTER')
        return NO_ACCESS_FILTER

    unique = list(dict.fromkeys(principals))
    joined = " OR ".join(f'"{principal}"' for principal in unique)
    rc = f"OwnerPrincipal:({joined})"
    _logger.info('🔒 Owner principal filter built: %r', rc)
    return rc
