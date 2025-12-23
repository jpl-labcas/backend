"""Service for downloading files."""

from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from datetime import timedelta
from functools import lru_cache
from typing import Any

from ..auth.dependencies import SecurityContext
from ..config import Settings, get_settings
from ..storage.s3 import S3ClientFactory
from ..utils.security import ensure_safe_value
from .query import QueryService, get_query_service

LOG = logging.getLogger(__name__)

# Solr field names
SOLR_FIELD_FILE_LOCATION = "FileLocation"
SOLR_FIELD_FILE_NAME = "FileName"
SOLR_FIELD_NAME = "name"


@dataclass
class FileInfo:
    """File information extracted from Solr document."""

    file_location: str
    file_name: str
    real_file_name: str
    file_path: str


class DownloadService:
    """Service for downloading files by ID."""

    def __init__(
        self,
        *,
        settings: Settings | None = None,
        query_service: QueryService | None = None,
        s3_client_factory: S3ClientFactory | None = None,
    ) -> None:
        self.settings = settings or get_settings()
        self.query_service = query_service or get_query_service()
        self.s3_client_factory = s3_client_factory or S3ClientFactory(settings=self.settings)

    async def get_file_info(self, *, security: SecurityContext, file_id: str) -> FileInfo | None:
        """Query Solr for file information by ID."""

        # Escape the file_id for Solr query syntax
        # Replace quotes and backslashes with escaped versions
        escaped_id = file_id.replace("\\", "\\\\").replace('"', '\\"')
        
        # Query Solr files core for the specific file ID
        params = {
            "q": f'id:"{escaped_id}"',
            "fl": f"{SOLR_FIELD_FILE_LOCATION},{SOLR_FIELD_FILE_NAME},{SOLR_FIELD_NAME}",
            "rows": 1,
        }

        try:
            result = await self.query_service.query_files(security=security, params=params)
            docs = result.get("response", {}).get("docs", [])

            if not docs:
                LOG.info("File ID %s not found", file_id)
                return None

            doc = docs[0]
            file_location = doc.get(SOLR_FIELD_FILE_LOCATION, "")
            file_name = doc.get(SOLR_FIELD_FILE_NAME, "")
            real_file_name = file_name

            # Override with name field if available
            name_field = doc.get(SOLR_FIELD_NAME)
            if name_field:
                if isinstance(name_field, list) and len(name_field) > 0:
                    first_name = name_field[0]
                    if first_name and isinstance(first_name, str) and len(first_name) > 0:
                        real_file_name = first_name
                elif isinstance(name_field, str) and len(name_field) > 0:
                    real_file_name = name_field

            file_path = f"{file_location}/{real_file_name}"

            LOG.info(
                "Resolved file ID %s: location=%s, fileName=%s, realFileName=%s, path=%s",
                file_id,
                file_location,
                file_name,
                real_file_name,
                file_path,
            )

            return FileInfo(
                file_location=file_location,
                file_name=file_name,
                real_file_name=real_file_name,
                file_path=file_path,
            )
        except Exception as exc:
            LOG.exception("Error querying file ID %s", file_id)
            raise

    def get_s3_presigned_url(self, s3_key: str, expiration_seconds: int | None = None) -> str:
        """Generate a presigned S3 URL for downloading a file."""

        if expiration_seconds is None:
            expiration_seconds = self.settings.aws_download_url_expiration_seconds

        s3_client = self.s3_client_factory.create()
        bucket = self.settings.s3_bucket

        if not bucket:
            raise ValueError("S3_BUCKET configuration is required for S3 downloads")

        LOG.info("Generating presigned URL for bucket=%s, key=%s, expiration=%s seconds", bucket, s3_key, expiration_seconds)

        url = s3_client.generate_presigned_url(
            "get_object",
            Params={"Bucket": bucket, "Key": s3_key},
            ExpiresIn=expiration_seconds,
        )

        LOG.info("Generated presigned URL: %s", url)
        return url

    def extract_s3_key(self, file_path: str) -> str:
        """Extract S3 key from a file path of the form s3://bucket/key..."""

        # Remove s3:// prefix and split
        if file_path.startswith("s3://"):
            parts = file_path[5:].split("/", 1)
            if len(parts) > 1:
                # Return everything after the bucket name
                return parts[1]
            return ""
        return file_path

    def is_local_file(self, file_location: str) -> bool:
        """Check if file location indicates a local file (not S3)."""

        return not file_location.startswith("s3")

    def apply_path_prefix_replacements(self, file_path: str) -> str:
        """Apply path prefix replacements from settings for debugging/local development."""

        if not self.settings.file_path_prefix_replacements:
            return file_path

        result_path = file_path
        replacements = self.settings.file_path_prefix_replacements.split(",")

        for replacement in replacements:
            replacement = replacement.strip()
            if ":" not in replacement:
                LOG.warning("Invalid path prefix replacement format (expected 'old:new'): %s", replacement)
                continue

            old_prefix, new_prefix = replacement.split(":", 1)
            old_prefix = old_prefix.strip()
            new_prefix = new_prefix.strip()

            if result_path.startswith(old_prefix):
                result_path = new_prefix + result_path[len(old_prefix) :]
                LOG.info("Applied path prefix replacement: %s -> %s", file_path, result_path)

        return result_path

    def get_file_size(self, file_path: str) -> int:
        """Get the size of a local file."""

        try:
            return os.path.getsize(file_path)
        except OSError as exc:
            LOG.warning("Could not get file size for %s: %s", file_path, exc)
            raise

    def get_media_type(self, file_path: str) -> str:
        """Determine the media type based on file extension."""

        lower_path = file_path.lower()
        if lower_path.endswith(".dcm") or "dicom" in lower_path:
            return "application/dicom"
        return "application/octet-stream"


@lru_cache(maxsize=1)
def _cached_download_service() -> DownloadService:
    return DownloadService()


def get_download_service() -> DownloadService:
    """FastAPI dependency hook for the download service."""

    return _cached_download_service()

