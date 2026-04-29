"""FastAPI dependencies for authentication and authorization."""

from __future__ import annotations

import base64, logging

from dataclasses import dataclass
from typing import List, Optional

from fastapi import Depends, HTTPException, Request, status

from ..config import Settings, get_settings
from ..directory import DirectoryProvider, LdapDirectoryProvider, MockDirectoryProvider
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


def _security_context_from_jwt(token: str, jwt_manager: JwtManager) -> SecurityContext:
    payload = jwt_manager.verify_token(token)
    subject = payload.get("sub")
    if not subject:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token.")
    groups: List[str] = []
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
            return _security_context_from_jwt(token, jwt_manager)
        except Exception:
            # If token verification fails, fall through to guest access
            pass
    
    # Try to get JWT from cookie (matching Java implementation)
    cookie = _get_jwt_from_cookies(request)
    if cookie:
        try:
            return _security_context_from_jwt(cookie, jwt_manager)
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
    """Authenticate via Bearer token, legacy JWT cookie, or Basic auth."""

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
            return _security_context_from_jwt(token, jwt_manager)
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid or expired token: {str(exc)}",
            ) from exc

    # Match the Java backend's download behavior for browser requests that omit Authorization.
    cookie_token = _get_jwt_from_cookies(request)
    if cookie_token:
        try:
            return _security_context_from_jwt(cookie_token, jwt_manager)
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
            
            # Return security context with user DN
            groups: List[str] = []
            return SecurityContext(subject=user.dn, groups=groups, token=None)
            
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


