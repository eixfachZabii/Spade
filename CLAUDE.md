# Spade: rules for agents

Spade is an AI poker dealer for **our own poker nights**: real cards on the table, each phone reads its owner's hole cards, a table camera reads the board, bets are tapped in, and Spade runs the hand, decides the showdown and keeps the ledger. Goals and principles: [PRODUCT.md](PRODUCT.md). Words: [CONTEXT.md](CONTEXT.md). Every doc: [docs/README.md](docs/README.md). Commands: [USAGE.md](USAGE.md).

## Before you write code: does it already exist?
1. **Issues and the roadmap.** `gh issue list --repo eixfachZabii/Spade --state all --search "<words>"` and [docs/handoffs/INDEX.md](docs/handoffs/INDEX.md).
2. **The status quo.** [docs/status-quo/](docs/status-quo/README.md) is the dated before-picture: what works, what is partial, what is dead. Read the file for your area first. The [salvage map](docs/status-quo/salvage.md) says what the rebuild keeps.
3. **The references.** [docs/references/](docs/references/README.md) holds what earlier Spade repos already solved: legacy API contracts, user stories, hand-evaluation vectors, the ledger, the voice-clip manifest, the webapp's style.
4. **The decisions.** [docs/adr/](docs/adr/). Propose a new ADR rather than silently contradicting one.

## Where the work lives
| What | Where |
|---|---|
| Ideas, bugs, the backlog | GitHub issues on the **Spade** board, filed with the `capture-idea` skill |
| Scoped work | [docs/handoffs/INDEX.md](docs/handoffs/INDEX.md), then one `PHASE_NN_YYYY-MM-DD_SLUG.md` per phase |
| Shipped work | `docs/handoffs/done/VersionN.0/` |
| Why it is this way | [docs/adr/](docs/adr/) |

## The repo
| Path | What | State |
|---|---|---|
| `spadeboot/` | Spring Boot 3 / Java 17: auth, lobby, tables, Hold'em engine, cheatsheet, Spotify | in use; rebuilt simpler in V1 |
| `cv/` | Python card detection (YOLOv8, socket.io), from `lucabzt/spadeAI` | in use; community cards are a stub |
| `client/`, `webapp/` | legacy React apps (hub, phone) | **frozen**: never edit; replaced in V1 by a new web hub and a native iOS player app ([ADR 0005](docs/adr/0005-rebuild-dont-repair.md)) |
| `scripts/` | the gate and its checks | |
| `docs/` | everything else | |

## Working in this repo: always in a worktree
Code edits on `master` in the main checkout are blocked by `.claude/hooks/guard-master.sh`; docs pass. One worktree per phase or fix:
```bash
git worktree add .worktrees/phase-NN-slug -b phase/NN-slug && cd .worktrees/phase-NN-slug
```
Merge back with the `ship-phase` skill. Deliberate one-liner on master: `SPADE_ALLOW_MASTER_EDIT=1`.

## The gate
```bash
scripts/gate.sh            # backend + cv + docs, then what it did NOT prove
scripts/gate.sh --backend  # or --cv, --docs
```
Green means:
- `./mvnw verify` passed;
- cv's ruff and pytest passed;
- every relative doc link resolves;
- CLAUDE.md is within its byte budget;
- the `scripts/` tests passed;
- no unpushed commit uses a negated close keyword.

**Read the "not proven" block every time.** The legacy apps, card-reading accuracy, real phones and cameras, and Docker are never covered. CI (`.github/workflows/gate.yml`) runs the same blocks on every push. Before claiming anything works: the `verify-change` skill.

## Secrets
They live only in the untracked `spadeboot/.env` (template `spadeboot/.env.example`), which `spadeboot/run.sh` loads. A missing `SPADE_JWT_SECRET` stops the backend at startup, on purpose.

## Conventions
- **Commits:** `type(scope): outcome (Phase NN, D#, #issue)`.
  - Types: feat · fix · docs · test · refactor · chore.
  - Scopes: backend · cv · docs · devex · legacy · security.
- **Branches:** `phase/NN-slug`, `fix/<issue>-slug`.
- **Decisions:** a phase doc numbers them D1…; commits cite them; lasting ones become ADRs.
- **Card notation:** three formats exist (cv `AS`/`10H`, backend `ACEH`-style, legacy client `THREES`-style). Don't add a fourth; see the `area:cv` notation issue.
- **Phase docs:** status line, "What shipped against what was scoped", "Not proven by this phase", "What shipping it actually found".

## Do not
- **Don't hand-type counts** (endpoints, tests, files) into any always-loaded doc. They go stale within a phase. Say how to derive them.
- **Don't create a `ROADMAP.md`**, or a checklist of future work in any doc. Future work is issues ([ADR 0003](docs/adr/0003-issues-are-the-inbox-index-is-the-roadmap.md)).
- **Don't put a secret** in a tracked file, commit message, issue or log line. Compare secrets by hash; never print them.
- **Don't edit `client/` or `webapp/`.**
- **Don't use `git stash`.** It is shared by every worktree.
- **Don't write a negated close keyword:** "not fixing #12" still closes #12. Write "Deferred: #12".
- **Don't save screenshots outside `.playwright/screenshots/`.**
- **Don't trust a plan over the code.** Plans go stale mid-phase; verify every claim against the code.
