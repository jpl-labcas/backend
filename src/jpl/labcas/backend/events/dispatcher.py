"""Lightweight synchronous event dispatcher."""

from __future__ import annotations

import logging
from collections import defaultdict
from typing import Callable, DefaultDict, Dict, Iterable, List, Protocol

LOG = logging.getLogger(__name__)


class EventListener(Protocol):
    """Protocol for event listeners."""

    def __call__(self, event: str, payload: Dict[str, object]) -> None:
        """Handle an event."""


class EventDispatcher:
    """Simple synchronous dispatcher for application events."""

    def __init__(self) -> None:
        self._listeners: DefaultDict[str, List[EventListener]] = defaultdict(list)

    def add_listener(self, event: str, listener: EventListener) -> None:
        self._listeners[event].append(listener)
        LOG.debug("Listener %s subscribed to event %s", listener, event)

    def dispatch(self, event: str, payload: Dict[str, object]) -> None:
        listeners = self._listeners.get(event, [])
        LOG.debug("Dispatching event %s to %d listener(s)", event, len(listeners))
        for listener in listeners:
            try:
                listener(event, payload)
            except Exception:  # noqa: BLE001
                LOG.exception("Event listener %s failed for event %s", listener, event)


