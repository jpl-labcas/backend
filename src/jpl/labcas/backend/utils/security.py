"""Security-related utilities."""

from __future__ import annotations

import re

# Unsafe characters: >, <, %, $ (matching Java implementation)
UNSAFE_PATTERN = re.compile(r"[<>%$\"'`]")


def ensure_safe_value(value: str) -> str:
    """Validate that a string does not contain unsafe characters."""

    if UNSAFE_PATTERN.search(value):
        msg = f"Unsafe characters detected in value: {value!r}"
        raise ValueError(msg)
    return value


