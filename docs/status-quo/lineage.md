# Lineage: where Spade came from, 2026-10-09

> Dated snapshot, not maintained (V0 spec §5).
> **Changed since:** 2026-10-09, after V0: the local `~/spade-archive/` bundles were deleted at the owner's request. The `add_first_game_logic` history no longer exists anywhere (it held secrets).
> Evidence: [audit](../handoffs/done/Version0.0/V0_audits/audit-lineage.md). Salvage verdicts: [salvage.md](salvage.md).

## Timeline
| When | Repo (local path) | What |
|---|---|---|
| Oct 2024 → May 2025 | `lucabzt/Spade` (`~/PycharmProjects/Spade`, HEAD `1510db9`; mirrored as `eixfachZabii/Spade-archived-`) | **The original.** Python game engine, Flask server, React `client/` (became the Vision UI hub on 2024-12-31), phone `webapp/` (Feb 2025), YOLO model, Spotify, lyrics, voice clips, UML |
| Mar 2025 | `M4RKUS28/SpadeBoot`, `eixfachZabii/SpringSpadeBoot` (`~/IdeaProjects/SEBA/…`) | Early Spring Boot experiments: a course template and AI-generated scaffolding. No lasting code |
| Mar 2025 | branch `add_first_game_logic` of this repo | First real Spring backend: JWT, roles, `HandEvaluation`. Archived 2026-10-09 to `~/spade-archive/` and deleted from GitHub (it held secrets) |
| Mar 2025 | `lucabzt/spadeAI` (`~/PycharmProjects/spadeAI`, `9e4ec5e`) | Card detection split out of the Flask server. Now [`cv/`](../../cv/README.md) |
| Jul 2025 | this repo (`eixfachZabii/Spade`, formerly `SpadeBoot`) | Fresh history: the Spring backend `spadeboot/` plus the evolved `client/` and `webapp/` |

There is no history anywhere for roughly April–July 2025, when the sessions engine, friends, Spotify and cheatsheet ports were written.

**Team (git authors):** Sebastian Rogg (eixfachZabii), Markus (M4RKUS28), Luca Bozzetti (lucabzt), with single commits from Matthias Meierlohr, paulv and Jonas Hoerter. A stale copy of the 2024 client also sits in `~/PycharmProjects/Hackathons & Projekte/SPADE/`.

## What survived into this repo
- The React frontends (frozen legacy).
- `HandEvaluation.java` (fixed in V0).
- The heatmap data, Spotify OAuth and lyrics, ported to Spring.
- The card-detection service, now `cv/`.

## What was lost on the way (still in `lucabzt/Spade@1510db9`)
| Asset | Path there | Status |
|---|---|---|
| 40 hand-analysis unit tests (categories, kickers, splits) | `server/src/tests/test_analysis/` | reference vectors ([hand-eval-vectors.md](../references/hand-eval-vectors.md)) |
| Equity / win-probability engine (vendored Holdem Calculator, MIT) | `server/src/engine/` | the only working win-probability code ever |
| 436 voice-dealer clips (ElevenLabs "Daniel": streets, actions, player names, hands, winners) | `server/assets/sounds/` | manifest in [voice-clips.md](../references/voice-clips.md) |
| Per-player P&L time series | `server/app.py` `/players` | replaced by the ledger idea |
| UML class and layered-architecture diagrams, target package tree, user stories | `UML`, `zUML.txt`, `zFolder.txt`, `zDone` | [references/](../references/README.md) |
| The group's ledger of 20 nights | `Poker_Chip_Tracker.xlsx` | [references/poker-chip-tracker.xlsx](../references/poker-chip-tracker.xlsx) |

## Where secrets leaked, and what happened
`lucabzt/Spade` still has old Spotify, Genius, Flask and Roboflow keys in its public history. The Spotify secret and JWT secret in use today differ from the leaked ones; the Genius token was revoked; Luca was told about the Roboflow key. This repo's own leaks were rotated and removed from history in V0 Phase 01.
