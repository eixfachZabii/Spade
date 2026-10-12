# Handoff Index

The only roadmap ([ADR 0003](../adr/0003-issues-are-the-inbox-index-is-the-roadmap.md)). Ideas live as issues on the "Spade" board; a version appears here once it is scoped. Versions newest first. Linking an issue inside a phase entry promotes it to `type:phase`.

## 🔲 Version 1 — scoped 2026-10-11 · **Dealer**

> **The version is one sentence:** the group plays its poker nights on Spade.
> Spec: [V1_2026-10-11_DEALER.md](V1_2026-10-11_DEALER.md) (decisions D1–D20, grilled 2026-10-10 → 11) · ADRs [0006](../adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md)–[0012](../adr/0012-v1-stacks-spring-boot-4-react-hub-swiftui-app.md) · Look: [DESIGN.md](../../DESIGN.md) · Evidence: [V1_evidence/](V1_evidence/README.md)
> **Evidence the grill decided from** (in [V1_evidence/](V1_evidence/README.md), 2026-10-09):
> 1. the card-detection spike (can the table camera read the board? how reliably do phones read hole cards?) 🔲 **proxy done**: [cv-proxy-spike.md](V1_evidence/cv-proxy-spike.md) on synthetic boards; the real spike ([#3](https://github.com/eixfachZabii/Spade/issues/3)) waits for the deck and the mat
> 2. the salvage map ✅ (V0)
> 3. an audit of WealthWatcher's backend and frontend architecture ✅ [wealthwatcher-architecture.md](V1_evidence/wealthwatcher-architecture.md)
> 4. iOS stack research (SwiftUI, camera, Core ML on-device detection, a realtime client) ✅ [ios-stack.md](V1_evidence/ios-stack.md), with a measured Core ML export
> 5. web-hub stack research, plus a design direction from the [webapp's style](../references/webapp-style.md) ✅ [web-hub-stack.md](V1_evidence/web-hub-stack.md), three directions in [design/](V1_evidence/design/DESIGN.md)

- **06 — Backend foundation** — ✅ merged 2026-10-12 — [doc](done/Version1.0/PHASE_06_2026-10-11_BACKEND_FOUNDATION.md) — `backend/` runs on Spring Boot 4.1: invite-code accounts, revocable device sessions, throttled logins, tables with buy-ins and counted cash-outs in points, the evaluator and showdown in model notation, problem+json errors, the testworld and the committed contract in `api/`; the gate and CI build it — [#5](https://github.com/eixfachZabii/Spade/issues/5), [#1](https://github.com/eixfachZabii/Spade/issues/1), [#2](https://github.com/eixfachZabii/Spade/issues/2)
- **07 — The hand** — 🔲 ready — [doc](PHASE_07_2026-10-11_THE_HAND.md) — a hand as a fold over events: side pots, corrections, chip counts; per-viewer snapshots over SSE — [#1](https://github.com/eixfachZabii/Spade/issues/1), [#2](https://github.com/eixfachZabii/Spade/issues/2)
- **08 — Player app** — 🔲 ready — [doc](PHASE_08_2026-10-11_PLAYER_APP.md) — SwiftUI on iOS 26: join, act, hole cards scanned on the phone or typed, the dealer's board, TestFlight — [#7](https://github.com/eixfachZabii/Spade/issues/7), [#4](https://github.com/eixfachZabii/Spade/issues/4)
- **09 — Hub** — 🔲 ready — [doc](PHASE_09_2026-10-11_HUB.md) — the table view in direction A, paired from a phone — [#6](https://github.com/eixfachZabii/Spade/issues/6)
- **10 — First night** — 🔲 ready — [doc](PHASE_10_2026-10-11_FIRST_NIGHT.md) — the owner's PC, Docker, a Cloudflare Tunnel, guests via TestFlight, a real night as the acceptance test — [#8](https://github.com/eixfachZabii/Spade/issues/8), [#9](https://github.com/eixfachZabii/Spade/issues/9)
- **11 — Ledger** — 🔲 ready — [doc](PHASE_11_2026-10-11_LEDGER.md) — counted cash-outs, the 20 old nights imported, P&L over nights — [#11](https://github.com/eixfachZabii/Spade/issues/11)
- **12 — Night extras** — 🔲 ready — [doc](PHASE_12_2026-10-11_NIGHT_EXTRAS.md) — cheatsheet and chip helper, win probability at all-in, Spotify's now-playing — [#14](https://github.com/eixfachZabii/Spade/issues/14), [#12](https://github.com/eixfachZabii/Spade/issues/12), [#13](https://github.com/eixfachZabii/Spade/issues/13)
- **13 — Table camera** — 🔲 **gated on [#3](https://github.com/eixfachZabii/Spade/issues/3)** — [doc](PHASE_13_2026-10-11_TABLE_CAMERA.md) — the camera as one more board source, only if the real spike passes; else Version 2

## ✅ Version 0 — shipped 2026-10-09 · **Foundation**

> **The version is one sentence:** Spade gets a working process before it gets new code.
> Closeout: [CLOSEOUT.md](done/Version0.0/CLOSEOUT.md) · Spec: [V0_2026-10-09_FOUNDATION.md](done/Version0.0/V0_2026-10-09_FOUNDATION.md) · Plan: [V0_2026-10-09_FOUNDATION_TASKS.md](done/Version0.0/V0_2026-10-09_FOUNDATION_TASKS.md) · Evidence: [V0_audits/](done/Version0.0/V0_audits/)

- **01 — Lockdown** — ✅ 2026-10-09 — secrets rotated and out of the repo, history rewritten, a fresh clone boots
- **02 — Shape** — ✅ merged 2026-10-09 (`ee01ad0`) — `cv/` imported, model in LFS, the ledger moved to references
- **03 — Gate** — ✅ merged 2026-10-09 (`7738881`) — `scripts/gate.sh` and CI green; evaluator fixed; guard hook
- **04 — Docs** — ✅ merged 2026-10-09 (`c8834e7`) — status quo, salvage map, references, ADRs 0001–0005, PRODUCT, CONTEXT, CLAUDE.md
- **05 — Tracker** — ✅ merged 2026-10-09 (`cc3e421`) — 15 labels, the Spade board (#3) with `check_board.py`, the ship-phase / capture-idea / verify-change skills, a seeded backlog of 16 issues ([#1](https://github.com/eixfachZabii/Spade/issues/1)–[#16](https://github.com/eixfachZabii/Spade/issues/16))
