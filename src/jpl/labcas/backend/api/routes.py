"""API route definitions."""

from __future__ import annotations

import base64
import logging
import os
from typing import Optional

from fastapi import APIRouter, Depends, Form, HTTPException, Query, Request, status
from fastapi.responses import JSONResponse, PlainTextResponse, RedirectResponse, StreamingResponse

from ..auth.dependencies import (
    GUEST_USER_DN,
    SecurityContext,
    get_directory_provider,
    get_jwt_manager,
    get_security_context,
    require_authenticated_user,
)
from ..auth.jwt_manager import JwtManager
from ..directory import DirectoryProvider
from ..services import (
    DownloadService,
    ListService,
    QueryService,
    get_download_service,
    get_list_service,
    get_query_service,
)
from ..utils.security import ensure_safe_value

LOG = logging.getLogger(__name__)


def create_router() -> APIRouter:
    """Assemble and return the application's root router."""

    router = APIRouter()
    data_router = APIRouter(prefix="/data-access-api")

    @router.get("/health", tags=["health"])
    async def healthcheck() -> dict[str, str]:
        return {"status": "ok"}

    @data_router.post(
        "/auth",
        response_class=PlainTextResponse,
        tags=["authentication"],
        summary="Authenticate user and get JWT",
        description="Authenticate using username/password via form data or HTTP Basic auth. Returns a JWT token.",
    )
    async def auth(
        request: Request,
        username: Optional[str] = Form(None),
        password: Optional[str] = Form(None),
        directory: DirectoryProvider = Depends(get_directory_provider),
        jwt_manager: JwtManager = Depends(get_jwt_manager),
    ) -> PlainTextResponse:
        """Authenticate user and return JWT token."""

        auth_username: Optional[str] = None
        auth_password: Optional[str] = None

        # Try to get credentials from form data first
        if username is not None and password is not None:
            auth_username = username
            auth_password = password
        else:
            # Try to extract from Authorization header (for Basic auth)
            auth_header = request.headers.get("Authorization")
            if auth_header and auth_header.startswith("Basic "):
                try:
                    encoded = auth_header.split(" ")[1]
                    decoded = base64.b64decode(encoded).decode("utf-8")
                    auth_username, auth_password = decoded.split(":", 1)
                except Exception as exc:
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="Invalid authentication header",
                        headers={"WWW-Authenticate": "Basic"},
                    ) from exc

        if not auth_username or not auth_password:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required",
                headers={"WWW-Authenticate": "Basic"},
            )

        # Authenticate user
        user = directory.authenticate(auth_username, auth_password)
        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid username or password",
                headers={"WWW-Authenticate": "Basic"},
            )

        # Generate JWT token with session ID
        subject = user.dn
        token = jwt_manager.issue_token(subject)
        LOG.info("Issued JWT token for subject=%s", subject)
        return PlainTextResponse(content=token, media_type="text/plain")

    @data_router.get(
        "/logout",
        tags=["authentication"],
        summary="Logout and invalidate session",
        description="Invalidate the current session and remove it from Redis.",
    )
    async def logout(
        sessionID: str = Query(..., description="Session ID to invalidate"),
        security: SecurityContext = Depends(require_authenticated_user),
        jwt_manager: JwtManager = Depends(get_jwt_manager),
    ) -> dict[str, str]:
        """Logout and invalidate the current session."""

        try:
            # Revoke session
            jwt_manager.revoke_session(sessionID)
            LOG.info("Revoked session %s for subject=%s", sessionID, security.subject)
            return {"status": "logged out", "message": "Session invalidated"}
        except Exception as exc:
            LOG.warning("Error during logout: %s", exc)
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Failed to logout: {str(exc)}",
            ) from exc

    @data_router.get(
        "/collections/list",
        response_class=PlainTextResponse,
        tags=["collections"],
        summary="List files within collections",
        description="The Collections List endpoint returns a list of files found within collections, one per line.",
    )
    async def collections_list(
        q: str = Query("*:*", description="Solr query"),
        fq: list[str] | None = Query(default=None, description="Filter queries"),
        start: int = Query(0, ge=0, description="Pagination start index"),
        rows: int = Query(1, ge=0, description="Number of collection rows to evaluate"),
        security: SecurityContext = Depends(get_security_context),
        list_service: ListService = Depends(get_list_service),
    ) -> PlainTextResponse:
        """Return download URLs for files contained in matching collections."""

        try:
            content = await list_service.list_collections(
                security=security,
                query=q,
                filters=fq or [],
                start=start,
                rows=rows,
            )
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

        return PlainTextResponse(content=content, media_type="text/plain")

    @data_router.get(
        "/datasets/list",
        response_class=PlainTextResponse,
        tags=["datasets"],
        summary="List files within datasets",
        description="The Datasets List endpoint returns a list of files found within datasets, one per line.",
    )
    async def datasets_list(
        q: str = Query("*:*", description="Solr query"),
        fq: list[str] | None = Query(default=None, description="Filter queries"),
        start: int = Query(0, ge=0, description="Pagination start index"),
        rows: int = Query(1, ge=0, description="Number of dataset rows to evaluate"),
        security: SecurityContext = Depends(get_security_context),
        list_service: ListService = Depends(get_list_service),
    ) -> PlainTextResponse:
        """Return download URLs for files contained in matching datasets."""

        try:
            content = await list_service.list_datasets(
                security=security,
                query=q,
                filters=fq or [],
                start=start,
                rows=rows,
            )
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

        return PlainTextResponse(content=content, media_type="text/plain")

    @data_router.get(
        "/files/list",
        response_class=PlainTextResponse,
        tags=["files"],
        summary="List files directly",
        description="The Files List endpoint returns a list of files based on a files core query, one per line.",
    )
    async def files_list(
        q: str = Query("*:*", description="Solr query"),
        fq: list[str] | None = Query(default=None, description="Filter queries"),
        start: int = Query(0, ge=0, description="Pagination start index"),
        rows: int = Query(1, ge=0, description="Number of file rows to evaluate"),
        security: SecurityContext = Depends(get_security_context),
        list_service: ListService = Depends(get_list_service),
    ) -> PlainTextResponse:
        """Return download URLs for files that match the given query."""

        try:
            content = await list_service.list_files(
                security=security,
                query=q,
                filters=fq or [],
                start=start,
                rows=rows,
            )
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc

        return PlainTextResponse(content=content, media_type="text/plain")

    def _extract_query_params(request: Request) -> dict[str, list[str] | str]:
        """Extract query parameters, handling multi-value parameters correctly."""
        params: dict[str, list[str] | str] = {}
        for key, values in request.query_params.multi_items():
            if key in params:
                # Convert to list if we already have this key
                existing = params[key]
                if isinstance(existing, str):
                    params[key] = [existing, values]
                else:
                    existing.append(values)
            else:
                params[key] = values
        return params

    @data_router.get(
        "/collections/select",
        tags=["collections"],
        summary="Query collections",
        description="Query the collections Solr core with access control applied. Returns Solr JSON response.",
    )
    async def collections_select(
        request: Request,
        security: SecurityContext = Depends(get_security_context),
        query_service: QueryService = Depends(get_query_service),
    ) -> JSONResponse:
        """Query collections core and return Solr JSON response."""

        # Extract all query parameters (handling multi-value params)
        params = _extract_query_params(request)

        try:
            result = await query_service.query_collections(security=security, params=params)
            return JSONResponse(content=result)
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
        except Exception as exc:
            LOG.exception("Error querying collections")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(exc)
            ) from exc

    @data_router.get(
        "/datasets/select",
        tags=["datasets"],
        summary="Query datasets",
        description="Query the datasets Solr core with access control applied. Returns Solr JSON response.",
    )
    async def datasets_select(
        request: Request,
        security: SecurityContext = Depends(get_security_context),
        query_service: QueryService = Depends(get_query_service),
    ) -> JSONResponse:
        """Query datasets core and return Solr JSON response."""

        # Extract all query parameters (handling multi-value params)
        params = _extract_query_params(request)

        try:
            result = await query_service.query_datasets(security=security, params=params)
            return JSONResponse(content=result)
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
        except Exception as exc:
            LOG.exception("Error querying datasets")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(exc)
            ) from exc

    @data_router.get(
        "/files/select",
        tags=["files"],
        summary="Query files",
        description="Query the files Solr core with access control applied. Returns Solr JSON response.",
    )
    async def files_select(
        request: Request,
        security: SecurityContext = Depends(require_authenticated_user),
        query_service: QueryService = Depends(get_query_service),
    ) -> JSONResponse:
        """Query files core and return Solr JSON response."""

        # jpl-labcas/backend#29
        # Require a logged-in user to query file metadata
        # Reject guest users explicitly (matching Java implementation)
        if not security.subject or security.subject == GUEST_USER_DN:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User login required to query file metadata (even for public data, so there!)",
            )

        # Extract all query parameters (handling multi-value params)
        params = _extract_query_params(request)

        try:
            result = await query_service.query_files(security=security, params=params)
            return JSONResponse(content=result)
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
        except Exception as exc:
            LOG.exception("Error querying files")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(exc)
            ) from exc

    @data_router.get(
        "/download",
        tags=["download"],
        summary="Download a file by ID",
        description="Download a file by its ID. Returns the file content or redirects to S3 presigned URL.",
        response_model=None,
    )
    async def download(
        id: str = Query(..., description="File ID to download"),
        suppressContentDisposition: bool = Query(False, description="Suppress Content-Disposition header"),
        security: SecurityContext = Depends(require_authenticated_user),
        download_service: DownloadService = Depends(get_download_service),
    ) -> StreamingResponse | RedirectResponse:
        """Download a file by ID."""

        # Validate ID is safe
        try:
            ensure_safe_value(id)
        except ValueError as exc:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="HTTP request contains unsafe characters",
            ) from exc

        try:
            # Get file information from Solr
            file_info = await download_service.get_file_info(security=security, file_id=id)

            if not file_info:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="File not found or not authorized",
                )

            LOG.info("Downloading file: location=%s, path=%s", file_info.file_location, file_info.file_path)

            # Handle S3 files
            if not download_service.is_local_file(file_info.file_location):
                s3_key = download_service.extract_s3_key(file_info.file_path)
                LOG.info("Generating S3 presigned URL for key=%s", s3_key)
                presigned_url = download_service.get_s3_presigned_url(s3_key)
                LOG.info("Redirecting to S3 URL: %s", presigned_url)
                return RedirectResponse(url=presigned_url, status_code=status.HTTP_307_TEMPORARY_REDIRECT)

            # Handle local files
            file_path = file_info.file_path
            # Apply path prefix replacements if configured (for debugging/local development)
            file_path = download_service.apply_path_prefix_replacements(file_path)
            
            if not os.path.exists(file_path):
                LOG.warning("File not found at path: %s", file_path)
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="File not found on server",
                )

            # Get file size and media type
            file_size = download_service.get_file_size(file_path)
            media_type = download_service.get_media_type(file_path)

            # Create file stream
            def generate():
                with open(file_path, "rb") as f:
                    while chunk := f.read(8192):
                        yield chunk

            # Build response headers
            headers = {"Content-Length": str(file_size)}
            if not suppressContentDisposition:
                # Escape filename for Content-Disposition header
                filename = file_info.file_name.replace('"', '\\"')
                headers["Content-Disposition"] = f'attachment; filename="{filename}"'

            LOG.info("Streaming file: path=%s, size=%s, mediaType=%s", file_path, file_size, media_type)
            return StreamingResponse(
                generate(),
                media_type=media_type,
                headers=headers,
            )

        except HTTPException:
            raise
        except ValueError as exc:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exc)) from exc
        except Exception as exc:
            LOG.exception("Error downloading file ID %s", id)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=str(exc),
            ) from exc

    router.include_router(data_router)
    return router


