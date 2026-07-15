"""AWS S3 helpers."""

from __future__ import annotations

import boto3

from ..config import Settings, get_settings


class S3ClientFactory:
    """Factory for creating S3 clients with configuration overrides."""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()

    def create(self):
        session = boto3.session.Session(
            aws_access_key_id=self.settings.aws_access_key_id,
            aws_secret_access_key=self.settings.aws_secret_access_key,
            region_name=self.settings.aws_region,
        )
        return session.client("s3")


