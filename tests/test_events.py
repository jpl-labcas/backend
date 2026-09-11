"""Unit tests for typed application events and the event dispatcher."""

from __future__ import annotations

import json
from datetime import datetime, timezone

from jpl.labcas.backend.auth.dependencies import GUEST_USER_DN, SecurityContext
from jpl.labcas.backend.events import (
    ANONYMOUS_PRINCIPAL,
    DownloadEvent,
    Event,
    EventDispatcher,
    principal_from_security,
)


def test_download_event_fields_and_json_string() -> None:
    timestamp = datetime(2024, 1, 2, 3, 4, 5, tzinfo=timezone.utc)
    event = DownloadEvent(
        principal="uid=alice,dc=example,dc=com",
        file_id="file-123",
        timestamp=timestamp,
    )

    assert event.principal == "uid=alice,dc=example,dc=com"
    assert event.file_id == "file-123"
    assert event.timestamp == timestamp

    payload = event.as_dict()
    assert payload == {
        "type": "DownloadEvent",
        "timestamp": "2024-01-02T03:04:05+00:00",
        "principal": "uid=alice,dc=example,dc=com",
        "file_id": "file-123",
    }
    assert json.loads(str(event)) == payload
    assert "\n" not in str(event)


def test_principal_from_security_maps_guest_to_anonymous() -> None:
    guest = SecurityContext(subject=GUEST_USER_DN, groups=[])
    user = SecurityContext(subject="uid=bob,dc=example,dc=com", groups=["g1"])

    assert principal_from_security(guest) == ANONYMOUS_PRINCIPAL
    assert principal_from_security(user) == "uid=bob,dc=example,dc=com"


def test_dispatcher_publishes_to_subscribed_listeners() -> None:
    dispatcher = EventDispatcher()
    received: list[Event] = []
    dispatcher.subscribe(received.append)

    event = DownloadEvent(principal="alice", file_id="f1")
    dispatcher.publish(event)

    assert received == [event]


def test_dispatcher_filters_by_event_type() -> None:
    from dataclasses import dataclass

    @dataclass(frozen=True, kw_only=True)
    class OtherEvent(Event):
        pass

    dispatcher = EventDispatcher()
    all_events: list[Event] = []
    download_events: list[Event] = []

    dispatcher.subscribe(all_events.append, Event)
    dispatcher.subscribe(download_events.append, DownloadEvent)

    download = DownloadEvent(principal="alice", file_id="f1")
    other = OtherEvent(principal="alice")
    dispatcher.publish(download)
    dispatcher.publish(other)

    assert all_events == [download, other]
    assert download_events == [download]


def test_raising_listener_does_not_block_others() -> None:
    dispatcher = EventDispatcher()
    received: list[Event] = []

    def boom(_event: Event) -> None:
        raise RuntimeError("listener failed")

    dispatcher.subscribe(boom)
    dispatcher.subscribe(received.append)

    event = DownloadEvent(principal="alice", file_id="f1")
    dispatcher.publish(event)

    assert received == [event]
