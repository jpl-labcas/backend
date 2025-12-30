# encoding: utf-8

'''Entrypoint for the LabCAS backend service.'''

from __future__ import annotations

import argparse
import ipaddress
import logging
import tempfile
from datetime import datetime, timedelta
from typing import Optional

import uvicorn
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from fastapi import FastAPI

from .api import create_router
from .config import Settings, get_settings, set_env_file
from .logging_config import configure_logging

LOG = logging.getLogger(__name__)


def generate_self_signed_certificate() -> tuple[str, str]:
    '''Generate a self-signed SSL certificate and key pair.
    
    Returns:
        Tuple of (keyfile_path, certfile_path) for temporary files.
    '''
    # Generate private key
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
    )
    
    # Create certificate
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, 'US'),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, 'California'),
        x509.NameAttribute(NameOID.LOCALITY_NAME, 'Pasadena'),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, 'JPL'),
        x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Rojeh's House of Encrypted Pancakes"),
        x509.NameAttribute(NameOID.COMMON_NAME, 'localhost'),
    ])
    
    cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        issuer
    ).public_key(
        private_key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        datetime.utcnow()
    ).not_valid_after(
        datetime.utcnow() + timedelta(days=365)
    ).add_extension(
        x509.SubjectAlternativeName([
            x509.DNSName('localhost'),
            x509.DNSName('*.localhost'),
            x509.IPAddress(ipaddress.IPv4Address('127.0.0.1')),
            x509.IPAddress(ipaddress.IPv6Address('::1')),
        ]),
        critical=False,
    ).sign(private_key, hashes.SHA256())
    
    # Write key and cert to temporary files
    key_file = tempfile.NamedTemporaryFile(mode='wb', delete=False, suffix='.key')
    cert_file = tempfile.NamedTemporaryFile(mode='wb', delete=False, suffix='.crt')
    
    try:
        key_file.write(
            private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption(),
            )
        )
        cert_file.write(cert.public_bytes(serialization.Encoding.PEM))
    finally:
        key_file.close()
        cert_file.close()
    
    return key_file.name, cert_file.name


def create_app(settings: Optional[Settings] = None) -> FastAPI:
    '''Create and configure the FastAPI application.'''

    settings = settings or get_settings()
    configure_logging(settings.log_level)

    app = FastAPI(
        title='LabCAS Data Access API',
        version='0.1.0',
        docs_url='/docs',
        redoc_url='/redoc',
    )

    app.state.settings = settings
    app.include_router(create_router())

    LOG.debug('FastAPI application created with environment=%s', settings.environment)
    return app


def cli(argv: Optional[list[str]] = None) -> None:
    '''Console script entry point.'''

    parser = argparse.ArgumentParser(description='Run the LabCAS backend service')
    parser.add_argument('--host', default=None, help='Host interface to bind')
    parser.add_argument('--port', type=int, default=None, help='Port to bind')
    parser.add_argument('--reload', action='store_true', help='Enable auto-reload (development only)')
    parser.add_argument('--tls', action='store_true', help='Enable TLS/SSL with self-signed certificate')
    parser.add_argument('--env', default=None, help='Path to .env file (overrides LABCAS_ENV_FILE and default locations)')

    args = parser.parse_args(argv)

    # Set env file from CLI if provided (must be done before get_settings())
    if args.env:
        set_env_file(args.env)

    settings = get_settings()
    # Configure logging early so log messages in cli() function are visible
    configure_logging(settings.log_level)
    LOG.info('🌞 Using Solr at %s (verify SSL=%r)', settings.solr_url, settings.solr_verify_ssl)
    host = args.host or settings.host
    port = args.port or settings.port

    # Configure SSL if enabled
    ssl_kwargs = {}
    if args.tls:
        keyfile_path, certfile_path = generate_self_signed_certificate()
        ssl_kwargs['ssl_keyfile'] = keyfile_path
        ssl_kwargs['ssl_certfile'] = certfile_path
        LOG.info('🔒 SSL enabled: Generated self-signed certificate (keyfile=%s, certfile=%s)', keyfile_path, certfile_path)

    LOG.info(
        '🏎️ Starting LabCAS backend on %s:%s with Redis at %s, LDAP at %s',
        host, port, settings.redis_url or '«unknown»', settings.ldap_uri or '«unknown»'
    )
    uvicorn.run(
        'jpl.labcas.backend.main:create_app',
        host=host,
        port=port,
        factory=True,
        reload=args.reload,
        **ssl_kwargs,
    )


if __name__ == '__main__':
    cli()


