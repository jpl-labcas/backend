"""Security-related utilities."""

from __future__ import annotations

import re

# Unsafe characters: >, <, %, $ (matching Java implementation)
UNSAFE_PATTERN = re.compile(r"[<>%$\"'`]")

# Common truthy spellings accepted for flexible query-string booleans.
_TRUTHY_VALUES = frozenset({"1", "true", "t", "yes", "y", "on"})


def ensure_safe_value(value: str) -> str:
    """Validate that a string does not contain unsafe characters."""

    if UNSAFE_PATTERN.search(value):
        msg = f"Unsafe characters detected in value: {value!r}"
        raise ValueError(msg)
    return value


def parse_truthy_query(value: str | bool | int | None) -> bool:
    """Return True for common truthy query-string values (``true``, ``True``, ``1``, etc.)."""

    if isinstance(value, bool):
        return value
    if value is None:
        return False
    if isinstance(value, int):
        return value != 0
    return str(value).strip().lower() in _TRUTHY_VALUES


