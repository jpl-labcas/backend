"""Typed synchronous event dispatcher."""

from __future__ import annotations

import logging
from functools import lru_cache
from typing import Protocol

from ..config import get_settings
from .event import Event
from .listeners import FileEventListener

LOG = logging.getLogger(__name__)


class EventListener(Protocol):
    """Protocol for event listeners."""

    def __call__(self, event: Event) -> None:
        """Handle an event."""


class EventDispatcher:
    """Simple synchronous dispatcher for typed application events."""

    def __init__(self) -> None:
        self._listeners: list[tuple[type[Event], EventListener]] = []

    def subscribe(self, listener: EventListener, event_type: type[Event] = Event) -> None:
        """Register a listener for events matching ``event_type`` (via isinstance)."""

        self._listeners.append((event_type, listener))
        LOG.debug("Listener %s subscribed to event type %s", listener, event_type.__name__)

    def publish(self, event: Event) -> None:
        """Notify all listeners whose subscribed type matches the event."""

        matching = [
            (event_type, listener)
            for event_type, listener in self._listeners
            if isinstance(event, event_type)
        ]
        LOG.debug(
            "Publishing %s to %d listener(s)",
            type(event).__name__,
            len(matching),
        )
        for event_type, listener in matching:
            try:
                listener(event)
            except Exception:  # noqa: BLE001
                LOG.exception(
                    "Event listener %s failed for event type %s",
                    listener,
                    type(event).__name__,
                )


@lru_cache(maxsize=1)
def _cached_event_dispatcher() -> EventDispatcher:
    settings = get_settings()
    dispatcher = EventDispatcher()
    dispatcher.subscribe(FileEventListener(settings.event_log_path))
    return dispatcher


def get_event_dispatcher() -> EventDispatcher:
    """FastAPI dependency hook for the event dispatcher."""

    return _cached_event_dispatcher()
