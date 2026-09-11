"""Unit tests for the file event listener."""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from jpl.labcas.backend.events import DownloadEvent, FileEventListener


def test_file_event_listener_appends_json_lines(tmp_path: Path) -> None:
    log_path = tmp_path / "nested" / "events.log"
    listener = FileEventListener(log_path)

    first = DownloadEvent(
        principal="alice",
        file_id="file-1",
        timestamp=datetime(2024, 1, 2, 3, 4, 5, tzinfo=timezone.utc),
    )
    second = DownloadEvent(
        principal="bob",
        file_id="file-2",
        timestamp=datetime(2024, 6, 7, 8, 9, 10, tzinfo=timezone.utc),
    )

    listener(first)
    listener(second)

    lines = log_path.read_text(encoding="utf-8").splitlines()
    assert len(lines) == 2
    assert json.loads(lines[0]) == first.as_dict()
    assert json.loads(lines[1]) == second.as_dict()
