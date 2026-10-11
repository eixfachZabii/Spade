# Phase 06 · Backend foundation

> **Status:** 🔲 ready, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Issues:** [#5](https://github.com/eixfachZabii/Spade/issues/5) (closes), [#1](https://github.com/eixfachZabii/Spade/issues/1) and [#2](https://github.com/eixfachZabii/Spade/issues/2) (in part; they close in phase 07)
> **Rests on:** V1 D3, D4, D10–D12, D14 · ADRs [0006](../adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md), [0010](../adr/0010-one-card-notation-the-models-labels.md), [0011](../adr/0011-device-sessions-not-jwts-the-hub-pairs.md), [0012](../adr/0012-v1-stacks-spring-boot-4-react-hub-swiftui-app.md)
> **Plan:** written with `superpowers:writing-plans` next to this doc.

## Outcome
`backend/` exists and runs:
- a player registers with the group's invite code and logs in once per device;
- a host opens a table, players sit down with a buy-in in points and leave with a counted cash-out;
- the evaluator passes its vectors in the model's card notation;
- the layer rules, the contract and the testworld hold from the first commit.

## Scope
### In
- **Project.** `backend/`: Spring Boot 4.1, Java 21, Maven wrapper, package root `com.spade`.
  - Layers: `api` (controllers, `api.dto` records), `service`, `repository` (entities live here), `domain` (pure), `config`, `security`.
  - Versions are checked against current docs at the start, not taken from this doc.
- **Architecture tests.** One ArchUnit test class with the rules of ADR 0012:
  - layers; domain purity; no entity in `api`; records in `api.dto`;
  - `*PublicView` can't name `HoleCards` (prepared for phase 07);
  - no field injection; no `System.out`;
  - testworld-only classes only under the `testworld` profile.
  - **Each rule is proven once by a planted violation** (WealthWatcher's habit), recorded in this doc's "What shipping found".
- **Errors.** One `@RestControllerAdvice` maps domain exceptions to RFC 9457 `ProblemDetail`. The catch-all never returns exception text and never answers 200 for an error (#2).
- **Config.**
  - `@ConfigurationProperties` records, validated at startup;
  - a `Clock` bean;
  - fail-fast on missing secrets (DB password, invite-code hash).
  - Secrets live in `backend/.env` (untracked; template `backend/.env.example`).
- **Persistence.**
  - PostgreSQL;
  - Flyway `V1__baseline.sql` (players, device sessions, tables, seats, buy-ins, cash-outs);
  - `ddl-auto: validate`;
  - Testcontainers PostgreSQL in tests;
  - **a migrate-from-empty test.**
- **Domain: cards.** `Rank`, `Suit`, `Card` as records/enums, parsed from and written as the model's labels (`AS`, `10H`; ADR 0010). Every layer rejects an unknown string.
- **Domain: evaluator.**
  - `HandEvaluator` ported from `spadeboot/…/HandEvaluation.java` as a pure function (no in-place sort of the caller's list).
  - It must pass **the 18 V0 vectors** (rewritten in model notation) **and the predecessor's two suites as rows**: `test_poker_hand_analyzer.py` and `test_poker_winner_analyzer.py`, from `lucabzt/Spade@1510db9`, listed in [hand-eval-vectors.md](../references/hand-eval-vectors.md).
  - The winner suite becomes a `Showdown.winners(...)` function: split pots and kickers.
- **Accounts** (ADR 0011):
  - `POST /api/v1/players` registers (username, display name, password, **invite code**);
  - `POST /api/v1/sessions` logs in and returns an opaque 256-bit token, whose SHA-256 is stored with the device name;
  - `GET /api/v1/players/me`;
  - `DELETE /api/v1/sessions/current` logs out;
  - `GET`/`DELETE /api/v1/players/me/devices` lists and revokes.
  - The token authenticates every request (`Authorization: Bearer`).
  - Login is throttled per account and per address.
- **Tables** (the legacy lobby rules, as concept):
  - a host opens a table (name, small/big blind in points, up to 10 seats);
  - a player joins with a buy-in in points; one open table per player;
  - a seated player tops up (another buy-in);
  - a player leaves with a counted cash-out in points;
  - the host closes the table when everyone has left. Running-hand checks arrive in 07.
  - The table view endpoint returns seats and stacks (no cards yet).
- **The testworld.** Profile `testworld`:
  - Flyway on a fresh database;
  - seeded players with documented test passwords (testworld only);
  - the invite code `testworld`;
  - a fixed `Clock`;
  - one open table;
  - a reset endpoint, existing only under the profile.
- **The contract.**
  - springdoc generates OpenAPI; a test writes `api/openapi.json` (repo root, committed).
  - **The gate fails when the generated spec differs from the committed one;** `-Dspade.contract.update=true` rewrites it.
  - Response bodies of the main endpoints under the testworld go to `api/fixtures/` (decoded by the clients in 08/09).
- **The gate and CI.**
  - `scripts/gate.sh --backend` builds `backend/`; it needs Docker for Testcontainers and says so if Docker isn't running.
  - `spadeboot/` leaves the gate and CI, frozen like `client/` and `webapp/`; a "not proven" line says so.
  - CI's backend job runs on `ubuntu-latest` with Docker.
- **Docs.** CLAUDE.md (repo table, secrets path, card-notation line → ADR 0010), USAGE.md (run the backend, the testworld, the contract update), CONTEXT if terms move.

### Out (and where it goes)
| What | Where |
|---|---|
| hands, betting, showdown, corrections, the SSE stream | 07 |
| hub pairing and the `HUB` role | 09 |
| ledger views, the 20-night import, P&L | 11 |
| Docker image, compose, the tunnel | 10 |
| deleting `spadeboot/` | V1 close |

## Decisions
| # | Decision |
|---|---|
| D1 | Folder `backend/`, package root `com.spade`. |
| D2 | **Registration needs the group's invite code.** Spade will be reachable from the internet (ADR 0007). The code is a secret in `.env`, compared by hash; the host gives it to guests. |
| D3 | **Amounts are whole points (`long`) everywhere in the backend.** Euros are computed only for the ledger (100 points = €1, one constant). |
| D4 | **Buy-ins and cash-outs are stored as rows from this phase on** (append-only). The ledger in 11 reads them; no migration churn later. |
| D5 | **`spadeboot/` stops being built by the gate** the moment `backend/` lands. The 18 vectors move with the evaluator. |
| D6 | `api/openapi.json` and `api/fixtures/` live at the repo root: the contract belongs to all three apps, not to the backend. |
| D7 | **Tests use Testcontainers PostgreSQL, not H2:** one database, the one that runs on the night. |

## Done when
- `scripts/gate.sh` is green, with:
  - every ArchUnit rule shown to fail once on a planted violation;
  - the migrate-from-empty test;
  - all evaluator rows;
  - the contract drift check.
- `verify-change` against the running app (testworld profile, `curl`): register (wrong invite code refused), log in, `/me` (no password hash in the body), list and revoke a device (the revoked token is refused at once), open a table, join with a buy-in, top up, leave with a cash-out, close.
- The same requests without a token get a `401 ProblemDetail`; a server error returns no stack trace or exception text.
- CI is green on the branch.

## Risks
- **Spring Boot 4.1 is young:** springdoc 3.x and Testcontainers support must be checked at the start.
- **Testcontainers on the Mac** needs Docker Desktop running for every gate run.
- **ArchUnit and generic type arguments** (`List<Entity>` in a record) need the planted-violation proof to be trusted.

## Not proven by this phase (expected)
- no client consumes the contract yet (08, 09);
- no real network or certificate (10);
- the Docker image is not built (10);
- `spadeboot/` is no longer built or tested.

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
