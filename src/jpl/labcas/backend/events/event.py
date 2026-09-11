"""Typed application events for the LabCAS event framework."""

from __future__ import annotations

import json
from abc import ABC
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..auth.dependencies import SecurityContext

ANONYMOUS_PRINCIPAL = "anonymous"


@dataclass(frozen=True, kw_only=True)
class Event(ABC):
    """Common ancestor for all application events."""

    principal: str
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    def _payload(self) -> dict[str, object]:
        """Return subclass-specific fields for serialization."""

        return {}

    def as_dict(self) -> dict[str, object]:
        """Return a JSON-serializable representation of this event."""

        payload: dict[str, object] = {
            "type": type(self).__name__,
            "timestamp": self.timestamp.isoformat(),
            "principal": self.principal,
        }
        payload.update(self._payload())
        return payload

    def __str__(self) -> str:
        return json.dumps(self.as_dict(), separators=(",", ":"))


@dataclass(frozen=True, kw_only=True)
class DownloadEvent(Event):
    """Event published when a file download is served."""

    file_id: str

    def _payload(self) -> dict[str, object]:
        return {"file_id": self.file_id}


def principal_from_security(security: SecurityContext) -> str:
    """Map a security context to an event principal string."""

    from ..auth.dependencies import GUEST_USER_DN

    if security.subject == GUEST_USER_DN:
        return ANONYMOUS_PRINCIPAL
    return security.subject
