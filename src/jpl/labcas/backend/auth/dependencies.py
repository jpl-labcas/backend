"""FastAPI dependencies for authentication and authorization."""

from __future__ import annotations

import base64, logging

from dataclasses import dataclass
from typing import List, Optional

from fastapi import Depends, HTTPException, Request, status

from ..config import Settings, get_settings
from ..directory import DirectoryProvider, DirectoryUser, LdapDirectoryProvider, MockDirectoryProvider
from .jwt_manager import JwtManager

_logger = logging.getLogger(__name__)
JWT_COOKIE_NAMES = ("token", "JasonWebToken")


@dataclass
class SecurityContext:
    """Security context attached to each request."""

    subject: str
    groups: List[str]
    token: Optional[str] = None


def get_directory_provider(settings: Settings = Depends(get_settings)) -> DirectoryProvider:
    """Resolve the configured directory provider."""

    # Better to use some kind of a factory pattern
    if settings.directory_provider == "mock":
        _logger.warning('⚠️ CAUTION: using mock directory provider')
        return MockDirectoryProvider()

    _logger.debug('🎉 Using LDAP directory provider!')
    return LdapDirectoryProvider(settings=settings)


def get_jwt_manager(settings: Settings = Depends(get_settings)) -> JwtManager:
    return JwtManager(settings=settings)


GUEST_USER_DN = "uid=guest,ou=public"


def _groups_for_subject(directory: DirectoryProvider, subject: str) -> List[str]:
    """Resolve LDAP group membership for an authenticated subject DN."""

    if subject == GUEST_USER_DN:
        return []

    return directory.get_groups(DirectoryUser(username=subject, dn=subject))


def _directory_user_for_subject(subject: str) -> DirectoryUser:
    return DirectoryUser(username=subject, dn=subject)


def _reject_if_pending(directory: DirectoryProvider, subject: str) -> None:
    """Raise 401 when the authenticated subject has a pending (unapproved) account."""

    if subject == GUEST_USER_DN:
        return
    if directory.is_pending(_directory_user_for_subject(subject)):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Account is pending approval",
            headers={"WWW-Authenticate": "Basic"},
        )


def _is_subject_pending(directory: DirectoryProvider, subject: str) -> bool:
    if subject == GUEST_USER_DN:
        return False
    return directory.is_pending(_directory_user_for_subject(subject))


def _security_context_from_jwt(
    token: str,
    jwt_manager: JwtManager,
    directory: DirectoryProvider,
    *,
    reject_pending: bool,
) -> SecurityContext | None:
    """Build a security context from a JWT.

    When ``reject_pending`` is True, pending accounts raise 401.
    When False (optional auth), pending accounts return ``None`` so callers can fall back to guest.
    """

    payload = jwt_manager.verify_token(token)
    subject = payload.get("sub")
    if not subject:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token.")
    if _is_subject_pending(directory, subject):
        if reject_pending:
            _reject_if_pending(directory, subject)
        return None
    groups = _groups_for_subject(directory, subject)
    return SecurityContext(subject=subject, groups=groups, token=token)


def _get_jwt_from_cookies(request: Request) -> str | None:
    for cookie_name in JWT_COOKIE_NAMES:
        token = request.cookies.get(cookie_name)
        if token:
            return token
    return None


async def get_security_context(
    request: Request,
    directory: DirectoryProvider = Depends(get_directory_provider),
    jwt_manager: JwtManager = Depends(get_jwt_manager),
) -> SecurityContext:
    """Get security context, allowing optional authentication (guest access when no token)."""

    auth_header = request.headers.get("Authorization")
    
    # Try to get JWT from Authorization header
    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header.removeprefix("Bearer ").strip()
        try:
            context = _security_context_from_jwt(
                token, jwt_manager, directory, reject_pending=False
            )
            if context is not None:
                return context
            # Pending account: fall through to guest access
        except Exception:
            # If token verification fails, fall through to guest access
            pass
    
    # Try to get JWT from cookie (matching Java implementation)
    cookie = _get_jwt_from_cookies(request)
    if cookie:
        try:
            context = _security_context_from_jwt(
                cookie, jwt_manager, directory, reject_pending=False
            )
            if context is not None:
                return context
            # Pending account: fall through to guest access
        except Exception:
            # If token verification fails, fall through to guest access
            pass
    
    # No valid authentication found, grant guest access
    return SecurityContext(subject=GUEST_USER_DN, groups=[], token=None)


async def require_authenticated_user(
    request: Request,
    directory: DirectoryProvider = Depends(get_directory_provider),
    jwt_manager: JwtManager = Depends(get_jwt_manager),
) -> SecurityContext:
    """Authenticate via Bearer token, legacy JWT cookie, or Basic auth.

    Pending (unapproved) accounts are rejected with 401.
    """

    auth_header = request.headers.get("Authorization")

    # Prefer explicit Bearer tokens when the frontend sends them.
    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header.removeprefix("Bearer ").strip()
        if not token:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required!",
            )

        try:
            context = _security_context_from_jwt(
                token, jwt_manager, directory, reject_pending=True
            )
            assert context is not None  # reject_pending=True always returns or raises
            return context
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid or expired token: {str(exc)}",
            ) from exc

    # Match the Java backend's download behavior for browser requests that omit Authorization.
    cookie_token = _get_jwt_from_cookies(request)
    if cookie_token:
        try:
            context = _security_context_from_jwt(
                cookie_token, jwt_manager, directory, reject_pending=True
            )
            assert context is not None
            return context
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid or expired token: {str(exc)}",
            ) from exc

    # Handle HTTP Basic Authentication after cookie JWT fallback.
    if auth_header and auth_header.startswith("Basic "):
        try:
            encoded = auth_header.split(" ", 1)[1]
            decoded = base64.b64decode(encoded).decode("utf-8")
            auth_username, auth_password = decoded.split(":", 1)
            
            # Authenticate user via directory
            user = directory.authenticate(auth_username, auth_password)
            if not user:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="🤔 Invalid username or password",
                    headers={"WWW-Authenticate": "Basic"},
                )

            _reject_if_pending(directory, user.dn)
            
            # Return security context with user DN
            groups = directory.get_groups(user)
            return SecurityContext(subject=user.dn, groups=groups, token=None)
            
        except HTTPException:
            raise
        except ValueError as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid Basic authentication header format",
                headers={"WWW-Authenticate": "Basic"},
            ) from exc
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Authentication failed: {str(exc)}",
                headers={"WWW-Authenticate": "Basic"},
            ) from exc

    if not auth_header:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required!",
            headers={"WWW-Authenticate": "Basic"},
        )

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid authentication header format. Use 'Basic' or 'Bearer'.",
        headers={"WWW-Authenticate": "Basic"},
    )


