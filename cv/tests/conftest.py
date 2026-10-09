"""Stop early, with one clear line, when the model is a Git LFS pointer.

Without this, a clone without git-lfs fails the model test with a long ultralytics
"not a loadable checkpoint" traceback, and the gate's tail never shows the fix.
"""
from pathlib import Path

import pytest

MODEL = Path(__file__).resolve().parent.parent / "models" / "best_60_23.pt"


def pytest_sessionstart(session):
    if MODEL.is_file() and MODEL.stat().st_size < 1_000_000:
        pytest.exit(
            f"cv/models/{MODEL.name} is a Git LFS pointer ({MODEL.stat().st_size} bytes): run `git lfs pull`",
            returncode=1,
        )
