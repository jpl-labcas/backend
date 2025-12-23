'''Service configuration.'''

from __future__ import annotations

from functools import lru_cache
from typing import Literal

from pydantic import Field, HttpUrl
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    '''Application settings loaded from environment variables.'''

    model_config = SettingsConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        case_sensitive=False,
        extra='ignore',
    )

    environment: str = Field('development', alias='LABCAS_ENV')
    host: str = Field('0.0.0.0', alias='LABCAS_HOST')
    port: int = Field(8000, alias='LABCAS_PORT')
    log_level: str = Field('INFO', alias='LABCAS_LOG_LEVEL')

    directory_provider: Literal['ldap', 'mock'] = Field('mock', alias='LABCAS_DIRECTORY_PROVIDER')
    ldap_uri: str | None = Field(None, alias='LABCAS_LDAP_URI')
    ldap_bind_dn: str | None = Field(None, alias='LABCAS_LDAP_BIND_DN')
    ldap_password: str | None = Field(None, alias='LABCAS_LDAP_PASSWORD')
    ldap_user_base: str | None = Field(None, alias='LABCAS_LDAP_USER_BASE')
    ldap_group_base: str | None = Field(None, alias='LABCAS_LDAP_GROUP_BASE')

    jwt_secret: str = Field('development-secret', alias='LABCAS_JWT_SECRET')
    jwt_issuer: str = Field('labcas', alias='LABCAS_JWT_ISSUER')
    jwt_audience: str = Field('labcas-clients', alias='LABCAS_JWT_AUDIENCE')
    session_ttl_seconds: int = Field(8 * 60 * 60, alias='LABCAS_SESSION_TTL_SECONDS')
    redis_url: str = Field('redis://localhost:6379/0', alias='LABCAS_REDIS_URL')

    search_engine: Literal['solr'] = Field('solr', alias='LABCAS_SEARCH_ENGINE')
    solr_url: HttpUrl | None = Field(None, alias='LABCAS_SOLR_URL')
    solr_max_rows: int = Field(5000, alias='LABCAS_SOLR_MAX_ROWS')
    solr_verify_ssl: bool = Field(True, alias='LABCAS_SOLR_VERIFY_SSL')
    download_base_url: HttpUrl = Field('http://localhost:8000/data-access-api/download', alias='LABCAS_DOWNLOAD_BASE_URL')
    super_owner_principal: str | None = Field(None, alias='LABCAS_SUPER_OWNER_PRINCIPAL')
    public_owner_principal: str | None = Field(None, alias='LABCAS_PUBLIC_OWNER_PRINCIPAL')

    aws_region: str | None = Field(None, alias='AWS_REGION')
    aws_access_key_id: str | None = Field(None, alias='AWS_ACCESS_KEY_ID')
    aws_secret_access_key: str | None = Field(None, alias='AWS_SECRET_ACCESS_KEY')
    s3_bucket: str | None = Field(None, alias='S3_BUCKET')
    aws_download_url_expiration_seconds: int = Field(20, alias='AWS_DOWNLOAD_URL_EXPIRATION_TIME_SECS')

    # Path prefix replacement for local file debugging (e.g., "/usr/local/labcas/backend:/Users/kelly")
    # Format: "old_prefix:new_prefix" (multiple replacements can be comma-separated)
    file_path_prefix_replacements: str | None = Field(None, alias='LABCAS_FILE_PATH_PREFIX_REPLACEMENTS')

    zipperlab_url: HttpUrl | None = Field(None, alias='LABCAS_ZIPPERLAB_URL')

    accept_any_jwt: bool = Field(False, alias='ACCEPT_ANY_JWT')

    ssl_keyfile: str | None = Field(None, alias='LABCAS_SSL_KEYFILE')
    ssl_certfile: str | None = Field(None, alias='LABCAS_SSL_CERTFILE')


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    '''Return a cached Settings instance.'''

    return Settings()


