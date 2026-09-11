"""Built-in event listeners."""

from __future__ import annotations

from pathlib import Path

from .event import Event


class FileEventListener:
    """Append stringified events to a log file."""

    def __init__(self, path: Path | str) -> None:
        self._path = Path(path)

    def __call__(self, event: Event) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        with self._path.open("a", encoding="utf-8") as handle:
            handle.write(f"{event}\n")
            handle.flush()
