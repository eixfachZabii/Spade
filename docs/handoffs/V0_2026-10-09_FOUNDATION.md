# Version 0 · Foundation: design

**Status:** 🔲 scoped 2026-10-09 (brainstorming, 6 sections approved one by one) · not started
**Plan:** `V0_2026-10-09_FOUNDATION_TASKS.md` (to be written by `superpowers:writing-plans` after this spec is approved)
**Evidence:** [`V0_audits/`](V0_audits/): five read-only audits from 2026-10-09

> **The version is one sentence:** Spade gets a working process before it gets new code.

> ⚠ **Do not push anything until Phase 01 step 1 (rotation) is done.** This doc and its audits name where secrets leaked. They are safe to publish only once those secrets are worthless.

---

## 1. Owner's brief

> *"read through wealth watcher first and anylze how it works. I want to setup the same working process for Spadeboot and get it on a next level I stopped working on it some time ago but now with new and better ai we can largerly improve it. I think we will entirely rebuild the frontend and cleanup the backend. But to properly do it we need issues phases docs status quo design goals references and more"*

The owner's answers during brainstorming, verbatim or as picked:

| Question | Answer |
|---|---|
| What is "next level"? | **"Our poker nights":** a tool the group actually uses at the table every poker night |
| How does a night work? | *"phone camera recognizes your own player cards. table cam reads community cards. chips like raise are entered manually. I want the card detection"* |
| Where does the CV service live? | **Monorepo** |
| Who builds it? | **Solo + AI** |
| How to sequence the relaunch? | **A:** foundation first, then grill V1 |

## 2. What the evidence says

| Audit | Main finding |
|---|---|
| [WealthWatcher docs/process](V0_audits/audit-wealthwatcher-docs-process.md) | Ideas are captured as issues, a version is grilled, the work is split into phase docs, each phase is built in a worktree, then `ship-phase` and a closeout. `INDEX.md` is the only roadmap ([ADR 0032 there](V0_audits/audit-wealthwatcher-docs-process.md) retired ROADMAP files). Copy the *shape*, not the 72–380 KB size |
| [WealthWatcher tooling](V0_audits/audit-wealthwatcher-tooling.md) | "Fix means build the guard." A single `gate.sh` with a "not proven" block, CI that mirrors it, 3–4 repo skills, a master-edit guard hook, labels plus a board. The 40 KB board-sync script, the contract-diff tooling and the finance rituals are overkill here |
| [Backend](V0_audits/audit-backend.md) | Lobby, friends and auth work. The in-memory Hold'em engine is broken: flushes are never detected, there are no side pots, results are never saved, every hand is sent to everyone, and the turn/winner/board events are never sent. 5 of 10 tests fail. No CV code |
| [Frontends](V0_audits/audit-frontends.md) | `client/` is a Vision UI template used as the TV hub. `webapp/` is the phone player app. socket.io points at Spring, which has no socket.io server, so card scanning is dead. Neither app has a winner or showdown screen. No tests |
| [Lineage](V0_audits/audit-lineage.md) | The Python era (`lucabzt/Spade`, `lucabzt/spadeAI`) holds what the relaunch needs: 62 hand-evaluation test vectors, an equity engine, 436 voice-dealer clips, the 20-night ledger spreadsheet, UML and user stories, and the YOLO model. Community-card detection was **never built** (it is a stub) |

Facts measured by hash comparison only (no values were printed):
- `gurobi.lic` in master history is a **Web License Service** licence (it contains an access ID and secret), so anyone can use it until the key is regenerated.
- A password on the orphan branch `add_first_game_logic` **is still in use** in the current local config.
- The JWT secret on that branch and the Spotify secret in Luca's repo are **not** the current ones.
- Neither `hub.poker-spade.de` nor `poker-spade.de` resolves: **nothing is deployed**.
- The repo has 0 forks.

## 3. Decisions

| # | Decision | By |
|---|---|---|
| D1 | The success measure is **used at every poker night**. When reliability and fast setup conflict with polish or scale, reliability and setup win | owner |
| D2 | **The play model:** a person deals physical cards. Each player's phone camera reads that player's own hole cards. A table camera reads the community cards. Bets and raises are entered by hand on the phone. Spade tracks the pot and the stacks, decides the showdown, announces, and records the result | owner |
| D3 | **Card detection is core scope**, not an optional extra | owner |
| D4 | **One repo:** `spadeboot/` + `cv/` + the (future) frontend. `cv/` is a snapshot copy of `lucabzt/spadeAI@9e4ec5e` with no history, crediting Luca. The model weights go in Git LFS | owner + derived |
| D5 | **Solo + AI:** one worktree per phase under `.worktrees/`, local `--no-ff` merges, no PRs | owner |
| D6 | **Sequencing:** V0 sets up the process and changes no product behaviour. V1 is grilled with `/grill-with-docs`, backed by a CV spike | owner |
| D7 | `client/` and `webapp/` are **frozen legacy**: no edits, not part of the gate, deleted when the V1 frontend replaces them. Their bugs are recorded in `status-quo/frontends.md`, not filed as issues | derived, approved |
| D8 | `spadeboot/` keeps its name, and the legacy apps stay where they are. A rename or move costs path churn and gains nothing | derived, approved |
| D9 | **No `ARCHITECTURE.md`, `REPO_FACTS.md`, `SEAMS.md`, `DECISIONS.md` or `DESIGN.md` in V0.** Each appears when it has something true and lasting to say (V1 or later) | derived, approved |
| D10 | `docs/handoffs/INDEX.md` is the **only roadmap**. GitHub issues are the inbox. No `ROADMAP.md` and no checklist of future work in any doc | derived from WW ADR 0032 |
| D11 | **Gate = backend + cv + docs**, plus a "not proven" block. No backend linting in V0: Checkstyle and ArchUnit arrive with the V1 cleanup | derived, approved |
| D12 | **V0 ships 3 repo skills:** `ship-phase`, `capture-idea` and `verify-change`. `add-surface` waits for V1, because it encodes wiring that depends on a frontend stack not chosen yet | derived, approved |
| D13 | **Tracker:** prefixed labels, a 6-column board, a small `check_board.py --fix`, and the issue body template in §8 | derived, approved |
| D14 | The known engine defects are filed as **one** issue carrying a checklist, not as about 20 separate bugs against code V1 replaces | derived, approved |
| D15 | **Security order:** rotate the secrets, then fix repo hygiene, then rewrite history. The history rewrite needs a **separate explicit yes** when it is reached | derived, approved |
| D16 | Code-level security holes become `area:security` issues for **V1**, because nothing is deployed | derived from measurement |
| D17 | The friends' photos and nicknames on the legacy About page stay until the legacy app is deleted in V1 | owner ("ok", read as "they're fine with it"; reversible) |
| D18 | V0 uses **one version doc plus one tasks doc**. Per-phase `PHASE_NN_…` docs start with V1. Phase numbers are global and never reset: V0 is phases 01–05 | derived, approved |
| D19 | The owner's uncommitted work goes in as its own commit before Phase 01. That work is the `chip20` change across 3 files, the H2 test dependency, and `CheatsheetService` using the local Gurobi licence file. The regenerated `*.pem` files and `.DS_Store` are **not** committed | derived, approved |
| D20 | The flush and straight-flush detection bug (`HandEvaluation.java:139`) is fixed in V0. The restored `HandEvaluatorTest` exposes it, and the gate has to be green | derived, approved |

**Assumptions, not yet confirmed (to settle in the V1 grill):**
- **A1.** Spade's digital stacks are the source of truth. Physical chips are optional or decorative.
- **A2.** The PRODUCT.md principles in §5 are proposals.
- **A3.** The V1 hosting is a laptop at the table on the home network, because the table camera needs a machine physically there.

## 4. Target repo layout after V0

```
SPADE/
├── CLAUDE.md          agent rulebook: lean (~8–10 KB), byte-capped, no hand-typed counts
├── AGENTS.md          pointer → CLAUDE.md
├── PRODUCT.md         who it's for, play model, principles, what Spade is not
├── CONTEXT.md         glossary
├── USAGE.md           commands only
├── README.md          rewritten: what Spade is, how to run it
├── spadeboot/         Spring Boot backend (boots cleanly; real cleanup in V1)
├── cv/                card-detection service (Python, uv), from lucabzt/spadeAI@9e4ec5e
├── client/ webapp/    frozen legacy frontends (D7)
├── docs/              §5
├── scripts/           gate.sh, check_doc_links.py, doc_budget.py, check_commit_refs.py, check_board.py
├── .claude/           settings.json (hooks), hooks/guard-master.sh, skills/{ship-phase,capture-idea,verify-change}
├── .github/workflows/ gate.yml
└── .worktrees/        gitignored
```

`spadeboot/src/main/resources/Poker_Chip_Tracker.xlsx` moves to `docs/references/` because no code reads it. The frontend folder name is chosen in the V1 grill.

## 5. Docs set

**Root:**

| File | Content in V0 |
|---|---|
| `PRODUCT.md` | Purpose (D1), how a night works (D2), the proposed principles: **the night never stops for Spade** (every camera read can be corrected by hand), **hole cards stay private until showdown**, **reliability beats magic**, **setup in minutes**, **the phone is the only per-player device**. Also what Spade is not (online poker, real-money gambling, a public product) and the open questions for V1 |
| `CONTEXT.md` | ~15 terms, written as `**Term** — definition. _Avoid_: synonyms`: poker night, table, seat, hand, street, hole cards, board, scan, correction, stack, bankroll, buy-in, cash-out, ledger, pot, side pot, showdown, hub |
| `CLAUDE.md` | Sections: pointer header · Spade at a glance · **does it already exist?** (check `references/`, `status-quo/` and issues first) · where work lives · worktree rule · how to run · the gate · structure · commit format · **Do not** list (§6) |
| `AGENTS.md` | A pointer, nothing else |
| `USAGE.md` | Commands only: run backend / cv / legacy apps, gate, skills, board |
| `README.md` | Public face: what Spade is, the play model in one paragraph, how to run it, a link to `docs/README.md` |

**`docs/`:**

```
docs/
├── README.md                map: every doc, who reads it
├── status-quo/              dated 2026-10-09: the "before" picture, never updated afterwards
│   ├── README.md            one page: works / partial / dead table, top risks
│   ├── backend.md  frontends.md  cv.md  lineage.md
├── references/              each asset carries its origin repo@sha and caveats
│   ├── README.md            index table: asset | origin | used for | caveats
│   ├── poker-chip-tracker.xlsx
│   ├── legacy-api.md        Flask routes, current Spring REST/STOMP, the spadeAI socket.io contract
│   ├── user-stories.md      the 8 stories, rewritten (never the SiemensGPT transcript or its code)
│   ├── uml/                 Spade UML + zUML (PlantUML sources)
│   ├── hand-eval-vectors.md 40 Python + 22 Java test cases, as data
│   └── voice-clips.md       manifest only (categories, counts, voice); clips stay out of the repo
├── adr/                     minimal format: title + context + decision + consequences
│   ├── 0001-one-repo-spadeboot-cv-frontend.md
│   ├── 0002-physical-cards-camera-reads-manual-betting.md
│   ├── 0003-issues-are-the-inbox-index-is-the-roadmap.md
│   └── 0004-solo-worktrees-local-merges.md
└── handoffs/
    ├── INDEX.md             the only roadmap (WW format: versions newest first, ✅ / 🔲 entries)
    ├── V0_2026-10-09_FOUNDATION.md  (this doc)  + _TASKS.md + V0_audits/
    └── done/Version0.0/     archive at closeout, with CLOSEOUT.md
```

**Safety rules:**
- The status-quo security section names the *kinds* of leak. Exact paths and commits appear only after the rotation (and are moot after the rewrite).
- The table reference photos stay out of the repo: they contain a strip of personal photo thumbnails.
- The voice clips stay out of the repo: they use friends' names.

## 6. Tooling and gate

**`scripts/gate.sh`:** one command. It collects every failure and ends with GREEN or RED.

| Block | Checks |
|---|---|
| backend | `cd spadeboot && ./mvnw -q verify`, against a committed secret-free config with an H2 test profile, so a fresh clone and CI both pass |
| cv | `uv sync --frozen`, `ruff check`, and a pytest smoke test: the model loads and exposes 52 card classes |
| docs | `check_doc_links.py`, `doc_budget.py check`, `check_commit_refs.py` (all three copied unchanged from WealthWatcher) |
| **not proven** | printed every run: legacy frontends untested · CV *accuracy* not measured · no real-phone or real-camera test · Docker image not built · a dirty tree means the working copy was checked, not the branch · "filed an issue? run `check_board.py --fix`" |

**Making it green:**
- Restore `HandEvaluatorTest.java` (22 tests) from `origin/add_first_game_logic` and fix the flush bug (D20).
- Repair the `GameServiceTest` fixtures (`Player.user` is never set). Any case that only tests behaviour V1 replaces is disabled with `@Disabled("#<issue>")` instead.

**CI:** `.github/workflows/gate.yml`, triggered on every push and on `workflow_dispatch`, with a per-ref concurrency group.
- **backend:** Temurin 17 with a Maven cache. Gurobi `12.0.2:core` resolves from Maven Central.
- **cv:** `actions/checkout` with `lfs: true`, plus uv. **Risk:** ultralytics pulls in torch. Pin the CPU-only torch index in `pyproject.toml` so CI doesn't download CUDA wheels.
- **docs:** the three scripts.

**`.claude/`:**
- `settings.json` (committed) wires `guard-master.sh` as a PreToolUse hook on Edit/Write. On master in the main checkout it blocks edits under `spadeboot/src`, `cv/`, `client/src` and `webapp/src`. Docs and worktrees pass. Bypass: `SPADE_ALLOW_MASTER_EDIT=1`.
- No impeccable hooks until V1.
- `.gitignore` gains `.claude/*` with `!` exceptions for `settings.json`, `hooks/` and `skills/`, plus `.worktrees/`, `.playwright/`, `.superpowers/` and `*.pem`/`.DS_Store` at every depth.

**Skills** (adapted from WealthWatcher, with WealthWatcher's incidents, issue numbers and finance traps stripped out):

| Skill | Steps |
|---|---|
| `ship-phase` | 1. gate, and read the "not proven" block · 2. truth up the docs against the code · 3. mark the phase ✅ in INDEX · 4. `git mv` the phase docs to `done/VersionN.0/` and fix links in both directions, then run `check_doc_links.py` · 5. close issues with the closing comment · 6. `check_board.py --fix` · 7. commit (no negated close keywords) · 8. `merge --no-ff`, then gate on master · 9. push · 10. remove the worktree and branch. This replaces the ROADMAP step in the owner's global checklist |
| `capture-idea` | 1. search issues, INDEX, code, `references/` and `status-quo/` · 2. file the issue (or file-and-close it as `already-shipped`) with the §8 body · 3. add labels · 4. `check_board.py --fix` |
| `verify-change` | 1. revert the fix and watch the test fail, then restore it · 2. check the running app, not just the tests · 3. any CV claim needs accuracy measured on a fixture set · 4. **privacy check:** inspect what *another* player's client actually receives · 5. state what you ran and what it printed |

**Commit format:** `type(scope): outcome (Phase NN, D#, #issue)`.
- **Types:** feat · fix · docs · test · refactor · chore.
- **Scopes:** backend · cv · docs · devex · legacy · security.
- **Branches:** `phase/NN-slug`, `fix/<issue>-slug`.

**The CLAUDE.md "Do not" list:**
- no hand-typed counts
- no `ROADMAP.md`
- no secrets in tracked files
- no edits to `client/` or `webapp/`
- no `git stash` (it is shared across worktrees)
- no negated close keywords (write "Deferred: #12", never "not fixing #12")
- no screenshots outside `.playwright/screenshots/`

## 7. Tracker

**Labels:** the 9 GitHub default labels are deleted.

| Family | Values |
|---|---|
| `area:` | backend · cv · frontend · devex · docs · security |
| `type:` | bug · idea · phase · chore |
| `version:` | v0 · v1 · later |
| flags | `needs-grill` · `already-shipped` |

No milestones.

**Board:** a GitHub user project named "Spade", with a single Status field: **Inbox → Grilling → Ready → Building → Shipped · WontDo**.
- New issues are added by the board's built-in auto-add workflow (no PAT needed).
- `scripts/check_board.py --fix` (~50 lines) enforces three rules: every open issue is on the board with a status; issues closed as completed sit in Shipped; issues closed as not planned sit in WontDo.
- The owner first runs `gh auth refresh -s project`.

**Promotion:** an issue becomes a phase when INDEX links it *inside* a phase entry. It then gets `type:phase`, moves to Ready, and stays open until that phase ships.

## 8. Issue body template and seeded backlog

```markdown
**Owner, <date>:** *"<verbatim>"*      ← or **Found by:** <status-quo doc link>
**What exists.** <files/endpoints, paths in backticks>
**The problem / idea.** <behaviour, with evidence>
**Constraints.** <PRODUCT.md principles / ADRs>
**Open questions.** <what the grill must settle>
**Not this issue:** <neighbouring issues>
```

Closing comment: *"Shipped in Version N Phase NN (merge `sha`). <what changed>. Guarded by: <tests>."*

**Seeded backlog (Phase 05, about 20 issues).** V0's own work is tracked in this doc, not in issues.

- **`area:backend` `version:v1` `needs-grill`:** "Engine rebuild: a state machine fed by camera-read cards". Its checklist holds every known defect: no side pots or odd-chip handling · results never saved · turn/winner/board events never sent · min-raise equals the big blind · the action response is built before the action applies · a round-thread exception kills the hand · a table can be deleted mid-game · thread-per-game concurrency.
- **`area:security` `version:v1`, one issue each:** hole cards sent to every client (REST `/status` + STOMP) · password hash in `GET /players/me` · STOMP CONNECT without a token is accepted, and SUBSCRIBE is not authorised · seeded admin in every profile · public, unauthenticated chip-optimiser endpoint (50 solver runs per request) · Spotify OAuth `state` not validated, and tokens in the redirect URL · `/users/{id}` and `/friends/{id}` expose data without an ownership check.
- **`area:cv`:**
  - community-card detection, which is the **V1 spike**;
  - phones need HTTPS for the camera, but the CV service speaks HTTP;
  - three card notations (`AS`/`10H`, `JACKH`, `THREES`) need one canonical form.
- **`area:frontend` `version:v1` `needs-grill`:** "Frontend rebuild", carrying the legacy feature list (what to keep and what to drop) and the known gaps: showdown/winner screen · all-in · configurable blinds · card-correction UI · what the TV may show.
- **`type:idea` `version:later` `needs-grill`:**
  - voice dealer (436 recorded clips vs live TTS);
  - ledger replacing the spreadsheet, importing the 20 nights;
  - win probability;
  - Spotify and lyrics: keep or drop;
  - cheatsheet, and the chip optimiser without Gurobi;
  - friends;
  - hand history and stats;
  - hosting (home LAN vs `hub.poker-spade.de`).
- **`area:devex`:** Docker (port/bind mismatch, ARM-only Gurobi, MySQL exposed on the host) · no DB migrations (`ddl-auto: update`) · dead dependencies and dead code.

## 9. Security

**Rotation.** The owner does this; I guide and generate locally where possible.

| Secret | Action |
|---|---|
| Gurobi WLS licence | **Most urgent.** Regenerate the API key in the Gurobi Web License Manager and replace the local `gurobi.lic` |
| DB password (the one shared with the orphan branch) | Change it in the local `.env` / config |
| Genius token | Regenerate it (the comparison was inconclusive, and it is cheap) |
| JWT secret | Generate a new one locally. The leaked one isn't in use, but the new config starts clean |
| TLS dev key | Already regenerated in the working copy. Never commit it |
| Spotify secret, old JWT, Flask key | Not needed: they differ from the current values or are dead |
| Roboflow key in `lucabzt/Spade` | Tell Luca. It isn't in this repo and isn't the owner's to rotate |

## 10. Phases

| Phase | Work | Done when |
|---|---|---|
| **pre** | D19 commit of the owner's pending work; this spec and the audits committed | `git status` shows only the `*.pem` and `.DS_Store` files that get untracked in 01 |
| **01 Lockdown** | 1. **rotate** (§9) · 2. **hygiene:** untrack `*.pem` + 7 `.DS_Store`; fix `.gitignore`; commit a secret-free `application.yml` with `${ENV}` placeholders + `.env.example`. The default profile boots on H2 **without SSL**; the keystore becomes opt-in via env. Restrict `DataInitializer` to `dev`, with passwords from env and never printed · 3. **rewrite (separate yes):** copy `HandEvaluatorTest.java` to `docs/references/legacy-tests/` (not compiled; Phase 03 moves it into the test tree); delete `origin/add_first_game_logic`; `git filter-repo` removes `gurobi.lic`, `*.pem`, `.DS_Store` from all history; force-push master | A fresh clone plus `.env.example` boots the backend. No secret in any reachable commit |
| **02 Shape** | Import `cv/` (snapshot, provenance README, `pyproject.toml` with uv, add the missing `eventlet` dependency, model via Git LFS); move the spreadsheet; root `.gitignore` | `cv/` starts locally and the model loads |
| **03 Gate** | `gate.sh`, the 3 doc scripts, `gate.yml`, `.claude/settings.json` + guard hook, restored and fixed tests (§6), cv smoke test | GREEN locally **and** CI green on master |
| **04 Docs** | All of §5, from the audits, every claim checked against the code | Link check green, doc budget set to the real CLAUDE.md size, every status-quo claim cites a path |
| **05 Tracker** | Labels, board, `check_board.py`, the 3 skills, seeded backlog (§8) | Every issue is on the board with a status. `capture-idea` has filed one real issue end to end |
| **Closeout** | `CLOSEOUT.md`; INDEX ✅; archive to `done/Version0.0/` | V0 is shipped **using its own `ship-phase`** |

**Phase 01 runs directly on master:** the history rewrite needs master, and the guard hook doesn't exist yet. From Phase 02 on, each phase runs in `.worktrees/phase-NN-slug` on the branch `phase/NN-slug` and merges `--no-ff`.

## 11. Handoff to V1

1. **Spike first, as evidence for the grill.** The question: *Can a table camera read the board at our table, and how reliably do phones read hole cards with `best_60_23.pt`?*
   - The owner provides the deck, the mat, the overhead camera and the lighting.
   - The result is a measured accuracy per scenario plus a recommendation.
   - The spike code is labelled throwaway.
2. **Then run `/grill-with-docs` on V1.** Topics already visible:
   - the engine as a state machine fed by card reads, with a correction flow;
   - the frontend: one app with phone and TV roles, or two; the stack; its `DESIGN.md` via `/impeccable`;
   - hosting (A3);
   - which extras survive;
   - A1 and A2.
3. **The grill's output:** the V1 name, `PHASE_06…` docs, the promoted issues, and ADRs and CONTEXT terms written as each decision is made.

## 12. Deliberately not done in V0

- Any product behaviour change, apart from D20's flush fix and the dev-only seeding that the secret-free config requires.
- The backend cleanup, the new frontend, CV accuracy work, the voice dealer, the ledger.
- Backend linting, ArchUnit, REPO_FACTS, ARCHITECTURE docs, DESIGN.md.
- `add-surface`, impeccable hooks, an e2e test setup.
- Changes to the owner's global `~/.claude/CLAUDE.md`. Its ROADMAP step is out of date, but the repo's `ship-phase` takes precedence.

## 13. Not proven by this spec / risks

- The history rewrite does not remove commits GitHub has already cached. They stay reachable by SHA until GitHub garbage-collects them. **Rotation is the actual protection.**
- The free Git LFS quota (1 GB storage and bandwidth) is fine for one 22.6 MB model, but every retrained model adds to storage.
- Repairing `GameServiceTest` may show that more of it tests behaviour V1 replaces than expected. The `@Disabled` + issue rule (§6) keeps the gate honest.
- The H2 test profile logs a DDL error for the `cards` table (`value` is a reserved word). The context still loads, but the plan should confirm it stays non-fatal.
- `spadeAI` has no licence file. Importing it relies on the owner having co-authored it (`9e4ec5e` is the owner's commit) and on Luca agreeing; a heads-up to Luca is part of Phase 02.
