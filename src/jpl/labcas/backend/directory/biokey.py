"""Parse LDAP description biokey metadata and pending-account status."""

from __future__ import annotations

import json
import logging
from typing import Any

LOG = logging.getLogger(__name__)

BIOKEY_MARKER = "@@biokey="


def parse_biokey(description: str | None) -> dict[str, Any] | None:
    """Extract and deserialize the ``@@biokey=…`` JSON object from a description string.

    Returns ``None`` when the marker is missing or the JSON cannot be parsed as an object.
    """

    if not description:
        return None

    marker_index = description.find(BIOKEY_MARKER)
    if marker_index < 0:
        return None

    payload = description[marker_index + len(BIOKEY_MARKER) :].strip()
    if not payload:
        return None

    # Prefer a leading JSON object; if surrounding text follows a complete object, decode
    # only the first value via raw_decode.
    try:
        value = json.loads(payload)
    except json.JSONDecodeError:
        try:
            value, _ = json.JSONDecoder().raw_decode(payload)
        except json.JSONDecodeError:
            LOG.debug("Failed to parse biokey JSON from description", exc_info=True)
            return None

    if not isinstance(value, dict):
        return None
    return value


def is_pending(biokey: dict[str, Any] | None) -> bool:
    """Return True only when biokey ``pending`` is exactly the string ``true``.

    Missing biokey, missing ``pending``, or any other value means the account is not pending.
    """

    if biokey is None:
        return False
    return biokey.get("pending") == "true"
