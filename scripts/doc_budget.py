#!/usr/bin/env python3
"""A ratchet for the documents that load into **every** session.

Copied from WealthWatcher on 2026-10-09; its incident notes are WealthWatcher's.

`CLAUDE.md` is not read on demand. It is prepended to every conversation this
repo has, so its size is a tax paid on every turn — and unlike the linters, no
tool prints a number, so nothing ever pushed back on it growing.

It went from ~55KB in May 2026 to **105,337 bytes / 896 lines** by August, and
the growth is not the interesting part. This is:

    98,916 → 102,411 bytes across the three commits that landed on
    2026-08-23, the same day issue #9 recorded "CLAUDE.md 110,212 → ~97,500;
    the rituals moved to five on-demand skills" as progress.

It grew back 3.5KB within hours of being trimmed, by three authors who each
added something individually reasonable. That is the signature of a rule that
only exists in prose: *"never add a count"*, *"keep it tight"* — real rules,
written down, and unenforceable, exactly like the linter baseline that
`lint_ratchet.py` exists to make mechanical.

So this is the same ratchet, pointed at bytes:

    python3 scripts/doc_budget.py check     # exit 1 if an always-on doc grew
    python3 scripts/doc_budget.py update    # accept the current sizes

**Bytes, not lines.** Lines are gameable by reflowing a table row, and the cost
being controlled is context, which is roughly proportional to bytes. A rewrite
that says the same thing in fewer bytes is exactly the win this wants.

**The ratchet turns one way.** Shrinking is free and reported as slack to lock
in. Growing fails, and the fix is to take something out — or to run `update`,
which is deliberate, one line, and shows the number going *up* in a diff where
a reviewer can ask why. That visibility is the whole mechanism: the budget is
not a hard ceiling, it is a receipt.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
BASELINE = REPO / "scripts" / "doc-budget.json"

# Documents prepended to every session, and therefore paid for on every turn.
#
# `USAGE.md`, `DESIGN.md`, `PRODUCT.md` and everything under `docs/` are
# deliberately NOT here: they are read on demand, which is the whole point of
# moving something into them. Adding a file to this list makes it expensive;
# that is a decision, not bookkeeping.
TRACKED = ["CLAUDE.md"]


class MeasurementFailed(RuntimeError):
    """The file could not be measured — as distinct from measuring as small.

    `lint_ratchet.py` learned this the hard way: a collector that fails to run
    and one that runs clean both produce "0", and writing that to a baseline
    silently un-ratchets the tool forever. A missing tracked file here would
    record a budget of 0 bytes, which then passes for eternity. Raise instead.
    """


def measure(rel: str) -> int:
    path = REPO / rel
    if not path.is_file():
        raise MeasurementFailed(
            f"{rel} does not exist. If it was renamed, update TRACKED in "
            f"{Path(__file__).name} — do not let it record as 0 bytes."
        )
    return len(path.read_bytes())


def load_baseline() -> dict[str, int]:
    if not BASELINE.exists():
        return {}
    text = BASELINE.read_text(encoding="utf-8")
    if "<<<<<<<" in text:
        # Same guidance as lint-baseline.json: a budget is a *measurement*, so
        # a conflict is never resolved by picking a side. Re-measure.
        raise SystemExit(
            f"{BASELINE.relative_to(REPO)} still has merge-conflict markers.\n"
            "  A budget is a measurement, not an opinion — do not pick a side:\n"
            "    git checkout --theirs scripts/doc-budget.json\n"
            "    python3 scripts/doc_budget.py update"
        )
    try:
        return {k: int(v) for k, v in json.loads(text).items()}
    except (json.JSONDecodeError, TypeError, ValueError) as exc:
        raise SystemExit(f"{BASELINE.relative_to(REPO)} is not valid JSON: {exc}") from exc


def save_baseline(sizes: dict[str, int]) -> None:
    BASELINE.write_text(json.dumps(sizes, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def check() -> int:
    baseline = load_baseline()
    if not baseline:
        print("no doc budget recorded — run `python3 scripts/doc_budget.py update`")
        return 1

    grew: list[tuple[str, int, int]] = []
    slack = 0
    for rel in TRACKED:
        now = measure(rel)
        was = baseline.get(rel)
        if was is None:
            grew.append((rel, 0, now))
        elif now > was:
            grew.append((rel, was, now))
        else:
            slack += was - now

    if grew:
        for rel, was, now in grew:
            delta = now - was
            print(f"  {rel} grew {was:,} → {now:,} bytes  (+{delta:,})")
        print(
            "\n  This file loads into every session, so the growth is paid on every turn.\n"
            "  Take something out — a lookup table belongs in docs/ with a pointer here,\n"
            "  which is what the API endpoints section already does at 1.2% of the file.\n"
            "  If the growth is genuinely worth it:\n"
            "      python3 scripts/doc_budget.py update    # the diff shows the number rising"
        )
        return 1

    total = sum(measure(r) for r in TRACKED)
    note = f", {slack:,} bytes reclaimed since baseline — run `update` to lock it in" if slack else ""
    print(f"always-on docs: {total:,} bytes, none grew{note}")
    return 0


def update() -> int:
    sizes = {rel: measure(rel) for rel in TRACKED}
    previous = load_baseline()
    save_baseline(sizes)
    for rel, now in sizes.items():
        was = previous.get(rel)
        if was is None:
            print(f"  {rel}: {now:,} bytes (new)")
        else:
            arrow = "↓" if now < was else ("↑" if now > was else "=")
            print(f"  {rel}: {was:,} → {now:,} bytes  {arrow}")
    print(f"\nWrote {BASELINE.relative_to(REPO)} — commit it.")
    return 0


def main() -> int:
    args = sys.argv[1:]
    cmd = args[0] if args else "check"
    try:
        if cmd == "check":
            return check()
        if cmd == "update":
            return update()
    except MeasurementFailed as exc:
        print(f"doc_budget: {exc}", file=sys.stderr)
        return 1
    print(__doc__)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
