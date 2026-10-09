#!/usr/bin/env python3
"""Every relative link in the docs resolves to a file that exists.

Copied from WealthWatcher (eixfachZabii/WealthWatchter) on 2026-10-09; its incident notes are WealthWatcher's.

CLAUDE.md spends a whole step of the "we are done" ritual on this, because
archiving a phase into `done/VersionN.0/` breaks every relative link inside the
moved file and every link pointing at it — and the ritual has been skipped:
*"Phase 42 sat with broken ADR links because an earlier move skipped exactly
this step."* Checking it costs 40ms; remembering to check it costs a phase.

Code-aware on purpose. A naive checker reports twelve failures on this repo and
ten of them are `[n](url)` written inside backticks — the citation format
WealthWatcher's `grounded_news` emits, quoted as an example. A checker that cries wolf ten
times out of twelve gets ignored, which is worse than not having one.

    python3 scripts/check_doc_links.py          # exit 1 if anything is broken
    python3 scripts/check_doc_links.py --list   # also print what was checked
"""

from __future__ import annotations

import re
import sys
import urllib.parse
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

FENCE = re.compile(r"^\s*(```|~~~)")
CODE_SPAN = re.compile(r"`[^`\n]*`")
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")

SKIP_DIRS = {".git", "node_modules", ".venv", ".worktrees", ".impeccable", ".superpowers", "target", "build", ".pytest_cache"}


def strip_code(text: str) -> str:
    """Blank out fenced blocks and inline code spans, keeping line structure."""
    out, in_fence = [], False
    for line in text.splitlines():
        if FENCE.match(line):
            in_fence = not in_fence
            out.append("")
            continue
        out.append("" if in_fence else CODE_SPAN.sub("``", line))
    return "\n".join(out)


def markdown_files() -> list[Path]:
    files = []
    for path in REPO.rglob("*.md"):
        if SKIP_DIRS & set(path.relative_to(REPO).parts):
            continue
        files.append(path)
    return sorted(files)


def main() -> int:
    verbose = "--list" in sys.argv
    broken: list[tuple[Path, str, int]] = []
    checked = 0

    for md in markdown_files():
        text = strip_code(md.read_text(encoding="utf-8", errors="replace"))
        for lineno, line in enumerate(text.splitlines(), 1):
            for match in LINK.finditer(line):
                href = match.group(1)
                if href.startswith(("http://", "https://", "mailto:", "#", "<")):
                    continue
                target_path = urllib.parse.unquote(href.split("#")[0])
                if not target_path:
                    continue
                checked += 1
                if not (md.parent / target_path).resolve().exists():
                    broken.append((md, href, lineno))

    rel = lambda p: p.relative_to(REPO)  # noqa: E731
    if verbose:
        print(f"{len(markdown_files())} markdown files, {checked} relative links")

    if not broken:
        print(f"  ✓ {checked} relative links resolve")
        return 0

    print(f"  ✗ {len(broken)} broken relative link(s) of {checked}:")
    for md, href, lineno in broken:
        print(f"      {rel(md)}:{lineno}  ->  {href}")
    print(
        "\n  Archiving a phase moves it one level deeper: `../adr/…` becomes"
        "\n  `../../../adr/…`. Fix the moved file's own links AND every link to it."
    )
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
