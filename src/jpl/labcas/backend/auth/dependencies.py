"""FastAPI dependencies for authentication and authorization."""

from __future__ import annotations

import base64

from dataclasses import dataclass
from typing import List, Optional

from fastapi import Depends, HTTPException, Request, status

from ..config import Settings, get_settings
from ..directory import DirectoryProvider, LdapDirectoryProvider, MockDirectoryProvider
from .jwt_manager import JwtManager


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
        return MockDirectoryProvider()

    return LdapDirectoryProvider(settings=settings)


def get_jwt_manager(settings: Settings = Depends(get_settings)) -> JwtManager:
    return JwtManager(settings=settings)


GUEST_USER_DN = "uid=guest,ou=public"


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
            payload = jwt_manager.verify_token(token)
            subject = payload.get("sub")
            if subject:
                groups: List[str] = []
                return SecurityContext(subject=subject, groups=groups, token=token)
        except Exception:
            # If token verification fails, fall through to guest access
            pass
    
    # Try to get JWT from cookie (matching Java implementation)
    cookie = request.cookies.get("JasonWebToken")
    if cookie:
        try:
            payload = jwt_manager.verify_token(cookie)
            subject = payload.get("sub")
            if subject:
                groups: List[str] = []
                return SecurityContext(subject=subject, groups=groups, token=cookie)
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
    """Authenticate the request via Basic auth or JWT Bearer token. Raises 401 if no valid auth."""

    auth_header = request.headers.get("Authorization")
    if not auth_header:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required!",
            headers={"WWW-Authenticate": "Basic"},
        )

    # Handle HTTP Basic Authentication
    if auth_header.startswith("Basic "):
        try:
            encoded = auth_header.split(" ", 1)[1]
            decoded = base64.b64decode(encoded).decode("utf-8")
            auth_username, auth_password = decoded.split(":", 1)
            
            # Authenticate user via directory
            user = directory.authenticate(auth_username, auth_password)
            if not user:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid username or password",
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

    # Handle JWT Bearer token
    if auth_header.startswith("Bearer "):
        token = auth_header.removeprefix("Bearer ").strip()
        if not token:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required!",
            )

        try:
            payload = jwt_manager.verify_token(token)
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid or expired token: {str(exc)}",
            ) from exc

        subject = payload.get("sub")
        if not subject:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token.")

        groups: List[str] = []
        return SecurityContext(subject=subject, groups=groups, token=token)

    # Unsupported authentication method
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid authentication header format. Use 'Basic' or 'Bearer'.",
        headers={"WWW-Authenticate": "Basic"},
    )


