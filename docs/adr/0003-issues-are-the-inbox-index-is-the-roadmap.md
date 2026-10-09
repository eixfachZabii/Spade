# 3. Issues are the inbox; docs/handoffs/INDEX.md is the only roadmap

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/done/Version0.0/V0_2026-10-09_FOUNDATION.md) D10, D13 · adopted from WealthWatcher ADR 0032

## Context
WealthWatcher kept ROADMAP files next to its phase docs. When it measured them, 84% of their content duplicated phase docs, and two of their open checkboxes had already shipped. A list with no state goes stale.

## Decision
- Ideas, bugs and complaints are GitHub issues on the "Spade" board (Inbox → Grilling → Ready → Building → Shipped · WontDo), filed with the `capture-idea` skill.
- `docs/handoffs/INDEX.md` lists versions and phases. It is the only roadmap.
- An issue becomes a phase when an INDEX phase entry links it.
- No `ROADMAP.md`, and no checklist of future work in any doc.

## Consequences
- Every item has a state and a home.
- `scripts/check_board.py --fix` keeps the board honest, because GitHub's "item closed" automation proved unreliable in WealthWatcher.
- The owner's global "we are done" checklist mentions ROADMAP files; in this repo the `ship-phase` skill replaces it.
