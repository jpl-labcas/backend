# encoding: utf-8

'''Entrypoint for the LabCAS backend service.'''

from __future__ import annotations

import argparse
import logging
from typing import Optional

import uvicorn
from fastapi import FastAPI

from .api import create_router
from .config import Settings, get_settings
from .logging_config import configure_logging

LOG = logging.getLogger(__name__)


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

    args = parser.parse_args(argv)

    settings = get_settings()
    LOG.info('🌞 Using Solr at %s (verify SSL=%r)', settings.solr_url, settings.solr_verify_ssl)
    host = args.host or settings.host
    port = args.port or settings.port

    # Configure SSL if certificates are provided
    ssl_kwargs = {}
    if settings.ssl_keyfile and settings.ssl_certfile:
        ssl_kwargs['ssl_keyfile'] = settings.ssl_keyfile
        ssl_kwargs['ssl_certfile'] = settings.ssl_certfile
        LOG.info('🔒 SSL enabled: keyfile=%s, certfile=%s', settings.ssl_keyfile, settings.ssl_certfile)

    LOG.info('Starting LabCAS backend on %s:%s', host, port)
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


