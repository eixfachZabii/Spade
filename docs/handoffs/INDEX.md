# Handoff Index

The only roadmap ([ADR 0003](../adr/0003-issues-are-the-inbox-index-is-the-roadmap.md)). Ideas live as issues on the "Spade" board; a version appears here once it is scoped. Versions newest first. Linking an issue inside a phase entry promotes it to `type:phase`.

## 🔲 Version 1 — queued · name chosen in its grill

> **Rebuild, don't repair** ([ADR 0005](../adr/0005-rebuild-dont-repair.md)): a new web hub, a native iOS player app, a simpler backend, all built from the [salvage map](../status-quo/salvage.md).
> **Evidence before the grill:**
> 1. the card-detection spike (can the table camera read the board? how reliably do phones read hole cards?)
> 2. the salvage map ✅ (V0)
> 3. an audit of WealthWatcher's backend and frontend architecture
> 4. iOS stack research (SwiftUI, camera, Core ML on-device detection, a realtime client)
> 5. web-hub stack research, plus a design direction from the [webapp's style](../references/webapp-style.md)
>
> Then `/grill-with-docs`. No phase numbers or docs yet; its phases start at 06.

## 🔲 Version 0 — building · **Foundation**

> **The version is one sentence:** Spade gets a working process before it gets new code.
> Spec: [V0_2026-10-09_FOUNDATION.md](V0_2026-10-09_FOUNDATION.md) · Plan: [V0_2026-10-09_FOUNDATION_TASKS.md](V0_2026-10-09_FOUNDATION_TASKS.md) · Evidence: [V0_audits/](V0_audits/)

- **01 — Lockdown** — ✅ 2026-10-09 — secrets rotated and out of the repo, history rewritten, a fresh clone boots
- **02 — Shape** — ✅ merged 2026-10-09 (`ee01ad0`) — `cv/` imported, model in LFS, the ledger moved to references
- **03 — Gate** — ✅ merged 2026-10-09 (`7738881`) — `scripts/gate.sh` and CI green; evaluator fixed; guard hook
- **04 — Docs** — ✅ merged 2026-10-09 (`c8834e7`) — status quo, salvage map, references, ADRs 0001–0005, PRODUCT, CONTEXT, CLAUDE.md
- **05 — Tracker** — ✅ merged 2026-10-09 — 15 labels, the Spade board (#3) with `check_board.py`, the ship-phase / capture-idea / verify-change skills, a seeded backlog of 16 issues ([#1](https://github.com/eixfachZabii/Spade/issues/1)–[#16](https://github.com/eixfachZabii/Spade/issues/16))
