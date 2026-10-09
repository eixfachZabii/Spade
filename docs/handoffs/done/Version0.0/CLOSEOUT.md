# Version 0 · Foundation: closeout

**The version in one sentence:** Spade got a working process before it got new code.

Started and shipped 2026-10-09 · base `7fffe67` (the last 2025 commit, after the history rewrite) · last phase merge `cc3e421` · 30 commits (`git rev-list --count 7fffe67..cc3e421`).
Spec: [V0_2026-10-09_FOUNDATION.md](V0_2026-10-09_FOUNDATION.md) (decisions D1–D25, corrections C1–C8) · Plan: [V0_2026-10-09_FOUNDATION_TASKS.md](V0_2026-10-09_FOUNDATION_TASKS.md) · Evidence: [V0_audits/](V0_audits/).

> **Owner's brief:** *"read through wealth watcher first and anylze how it works. I want to setup the same working process for Spadeboot and get it on a next level […] we need issues phases docs status quo design goals references and more"*

## What it found
- **WealthWatcher's process** is capture → grill → phase docs → worktree → `ship-phase`, enforced by one gate that admits what it doesn't prove. Copy the shape, not the 72–380 KB docs.
- **The backend** worked for accounts and the lobby; its in-memory engine never saved results, never sent the winner, had no side pots, and sent every hole card to everyone.
- **The frontends** were a dashboard template (hub) and a phone app whose card scanner pointed at a server that didn't exist.
- **Card detection** lived in another repo; reading the board was never built.
- **The lineage** held what the relaunch needs: 40 Python test vectors, an equity engine, 436 voice clips, the 20-night ledger.
- **Secrets** were in the public history: a cloud Gurobi licence (usable by anyone), TLS keys, and a database password that was still in use.
- **Planning measured eight things the spec had wrong** (C1–C8). The most instructive: the old branch's "22 tests" had no assertions (C1), and the full-house ranking was wrong too (C2).

## What moved that the owner can see
| Before | After | Where |
|---|---|---|
| Secrets in the repo and its history | Rotated; only in the untracked `spadeboot/.env`; history rewritten, old branch archived locally | Phase 01 |
| A fresh clone could not boot | It boots on H2 with one env var, and refuses to start without the JWT secret | Phase 01 |
| Seed users with `admin123`, in every profile | Dev profile only, one random password from env, never logged | Phase 01 |
| Card detection in another repo | `cv/` in this repo, model in Git LFS, smoke tests | Phase 02 |
| Flushes and straight flushes never detected; full houses mis-ranked | Fixed, pinned by 18 vectors (8 fail without the fix) | Phase 03 |
| 5 of 10 tests failing, no CI | `scripts/gate.sh` green; CI runs backend, cv, docs and scripts on every push | Phase 03 |
| No rule against editing master directly | A hook blocks code edits on master in the main checkout | Phase 03 |
| A marketing README | Status quo, salvage map, references, five ADRs, PRODUCT, CONTEXT, CLAUDE.md, USAGE | Phase 04 |
| No backlog | 15 labels, the Spade board, 16 issues, three repo skills | Phase 05 |

## What left the codebase
The `info` file (it pointed to secrets in a Discord channel); the dev and prod config profiles (folded into env); the TLS keys and `.DS_Store` files, from every commit; the `add_first_game_logic` branch (bundled in `~/spade-archive/`); the spreadsheet from the backend's resources (now in `docs/references/`).

## Deferred, with reasons
| Item | Why not in V0 | Issue |
|---|---|---|
| Backend rebuild, engine as a state machine | V1, rebuild not repair (ADR 0005) | [#1](https://github.com/eixfachZabii/Spade/issues/1) |
| Security holes in today's backend | nothing is deployed; closed by design in the rebuild | [#2](https://github.com/eixfachZabii/Spade/issues/2) |
| Board reading, measured accuracy | needs the physical setup; the V1 spike | [#3](https://github.com/eixfachZabii/Spade/issues/3) |
| New hub, iOS player app | V1, after research and the grill | [#6](https://github.com/eixfachZabii/Spade/issues/6), [#7](https://github.com/eixfachZabii/Spade/issues/7) |
| Hosting, TLS on the local network, weak dev keystore password | V1 grill topic | [#8](https://github.com/eixfachZabii/Spade/issues/8) |
| Docker | depends on hosting | [#9](https://github.com/eixfachZabii/Spade/issues/9) |
| Board auto-add and Board layout | GitHub offers them only in the web UI; `check_board.py --fix` covers the gap | owner action |

## Process notes worth keeping
- **An audit's count is not a test.** "22 tests" had no assertions. Check for assertions before restoring anything (C1).
- **Measure secrets by hash, never print them.** The comparisons decided what needed rotating (the Spotify secret did not; a DB password did) without a value ever reaching a log.
- **`git filter-repo` turns remote-tracking branches into local ones.** After a rewrite, look for branches you didn't have, and purge them if they hold what you just removed.
- **Global tool hooks leave caches in the repo.** The impeccable hook wrote `.impeccable/` next to edited files and one slipped into a commit; it's in `.gitignore` now.
- **GitHub's project item list lags writes by up to a minute.** Wait before calling the board script broken.
- **Spring devtools swallows a startup failure's exit code.** "It refused to start" has to be read from the log, not from `$?`.
- **Executing a plan from worktrees:** call the plan tooling with the repo-relative plan path, and point each worktree's `.superpowers/` at the main one, or the ledger splits.

## Next
Version 1 is queued in [INDEX](../../INDEX.md): five pieces of evidence (the card-detection spike first, which needs the deck, the mat, an overhead camera and poker-night light), then `/grill-with-docs`. Still open from V0: does everyone in the group have an iPhone?

## Amendment: 2026-10-09 (owner cleanup after shipping)
- **Everyone in the group has an iPhone** (owner): the player app is iPhone-only, no fallback. Recorded in PRODUCT.md and on #7.
- **The local archives are gone.** `~/spade-archive/` (the `add_first_game_logic` bundle and the pre-rewrite master) and `~/spade-secrets-backup/` were deleted at the owner's request, after the commit IDs for a GitHub Support purge request had been extracted from them. "Bundled in `~/spade-archive/`" above is therefore historical: that history no longer exists.
- **Issue #8** no longer hints at the dev keystore password.
