"""Logging configuration utilities."""

from __future__ import annotations

import logging
from logging.config import dictConfig
from typing import Any, Mapping


def default_logging_config(level: str = "INFO") -> Mapping[str, Any]:
    """Return a logging configuration dictionary."""

    return {
        "version": 1,
        "disable_existing_loggers": False,
        "formatters": {
            "standard": {
                "format": "%(asctime)s %(levelname)s [%(name)s] %(message)s",
            }
        },
        "handlers": {
            "console": {
                "class": "logging.StreamHandler",
                "formatter": "standard",
                "level": level.upper(),
            }
        },
        "root": {
            "handlers": ["console"],
            "level": level.upper(),
        },
    }


def configure_logging(level: str = "INFO") -> None:
    """Configure application logging."""

    dictConfig(default_logging_config(level))
    logging.getLogger(__name__).debug("Logging configured at %s level", level)


