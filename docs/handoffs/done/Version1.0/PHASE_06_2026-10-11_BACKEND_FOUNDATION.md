# Phase 06 · Backend foundation

> **Status:** ✅ shipped 2026-10-12 (scoped 2026-10-11) · Version 1 · Dealer ([spec](../../V1_2026-10-11_DEALER.md))
> **Issues:** [#5](https://github.com/eixfachZabii/Spade/issues/5) (closes), [#1](https://github.com/eixfachZabii/Spade/issues/1) and [#2](https://github.com/eixfachZabii/Spade/issues/2) (in part; they close in phase 07)
> **Rests on:** V1 D3, D4, D10–D12, D14 · ADRs [0006](../../../adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md), [0010](../../../adr/0010-one-card-notation-the-models-labels.md), [0011](../../../adr/0011-device-sessions-not-jwts-the-hub-pairs.md), [0012](../../../adr/0012-v1-stacks-spring-boot-4-react-hub-swiftui-app.md)
> **Plan:** [PHASE_06_2026-10-11_BACKEND_FOUNDATION_TASKS.md](PHASE_06_2026-10-11_BACKEND_FOUNDATION_TASKS.md) (13 tasks, `superpowers:writing-plans`).

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
  - fail-fast on missing secrets (DB password, invite-code hash). *(Shipped for the invite-code hash only: see the first deviation.)*
  - Secrets live in `backend/.env` (untracked; template `backend/.env.example`).
- **Persistence.**
  - PostgreSQL;
  - Flyway migrations `V1__accounts.sql` (players, device sessions) and `V2__tables.sql` (tables, seats, buy-ins, cash-outs);
  - `ddl-auto: validate`;
  - Testcontainers PostgreSQL in tests;
  - **a migrate-from-empty test.**
- **Domain: cards.** `Rank`, `Suit`, `Card` as records/enums, parsed from and written as the model's labels (`AS`, `10H`; ADR 0010). Every layer rejects an unknown string.
- **Domain: evaluator.**
  - `HandEvaluator` ported from `spadeboot/…/HandEvaluation.java` as a pure function (no in-place sort of the caller's list).
  - It must pass **the 18 V0 vectors** (rewritten in model notation) **and the predecessor's two suites as rows**: `test_poker_hand_analyzer.py` and `test_poker_winner_analyzer.py`, from `lucabzt/Spade@1510db9`, listed in [hand-eval-vectors.md](../../../references/hand-eval-vectors.md).
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
  - **The gate fails when the generated spec differs from the committed one;** `SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest` rewrites it (an environment variable, read by `ContractTest`).
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
- CI is green on the branch (checked at ship time, after the push; not claimed here).

## Risks
- **Spring Boot 4.1 is young:** springdoc 3.x and Testcontainers support must be checked at the start.
- **Testcontainers on the Mac** needs Docker Desktop running for every gate run.
- **ArchUnit and generic type arguments** (`List<Entity>` in a record) need the planted-violation proof to be trusted.

## Not proven by this phase (expected)
- real phones and the hub: no client exists yet, nothing consumes the contract (08, 09);
- the Cloudflare Tunnel and the owner's PC: nothing is deployed (10), no real network or certificate;
- `CF-Connecting-IP` is trusted from any caller, so the backend must be reachable only through cloudflared. **Phase 10 must bind it so**; until then a caller can pick its own address and dodge the per-address throttles;
- throttle state lives in memory: a restart forgets every lockout;
- the Docker image is not built (10);
- `spadeboot/` is no longer built or tested (D5);
- the `/v3/api-docs` endpoint is public and on by default; whether it should be off in production is not decided here. `springdoc.api-docs.enabled: true` now says so in `application.yml`, which also silences springdoc's startup warning;
- `TestworldGuard` refuses a database with real players; it does not require a separate database. An empty database passes it (phase 10 carries this).

## What shipped, against what was scoped
Everything in "Scope / In" shipped as scoped, with one exception (the DB password has no fail-fast of its own, the first deviation below). The gate and CI shipped as scoped too: `scripts/gate.sh --backend` builds `backend/` and stops with a clear message when Docker isn't running; `spadeboot/` left the gate and CI with a "not proven" line; CI's backend job runs on `ubuntu-latest` with Docker and Java 21. The pieces the scope doesn't spell out, or that changed on the way, are the deviations below.

### `verify-change` against the running app
`(cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld)`, a throwaway Testcontainers PostgreSQL, curl from outside. Run 2026-10-12; the app was stopped afterwards and nothing listened on :8080. The rows for opening and closing a table, the unreadable body, the 401 without a header and the firewall's 400 were run again on the final code, after the final review's fix wave; the others ran earlier, on behaviour that wave did not touch.

| Request | Expected | Got |
|---|---|---|
| `POST /players`, invite code `wrong` | 403 problem | 403, "That invite code isn't ours. Ask the host for the group's code." |
| `POST /players`, invite code `testworld` | 201, no password in body | 201, body is `id`, `username`, `displayName` only |
| `POST /sessions`, username `ZOE` (case differs) | 201 with a token | 201, token of 43 characters |
| `GET /players/me` with the token | 200 | 200, zoe |
| `GET /players/me` with `Bearer nope`, and with no header | 401 problem | 401 problem both. The sentence `Sign in first: this needs a valid session.` was seen on the re-run without a header; a bad token reaches the same entry point (`BearerSessionFilter` leaves it anonymous), and `ApiErrorsTest` pins the sentence |
| `POST /tables/1/seats` buy-in 1003 | 400, steps of 5 | 400 "A buy-in must be in steps of 5 points (the smallest chip), got 1003." |
| `POST /tables/1/seats` buy-in 1000 | 201, seat 1 | 201, seat 1, stack 1000 |
| `POST /tables/1/seats/me/buy-ins` 500 | 200, stack 1500 | 200, stack 1500 |
| `POST /tables/1/seats/me/cash-out` 1730 | 200, seat gone | 200, `seats: []` |
| `DELETE /players/me/devices/{id}` | 204 | 204 |
| `GET /players/me` with the revoked token | 401 at once | 401 |
| `GET /actuator/health` | UP | 200, `{"groups":["liveness","readiness"],"status":"UP"}` |
| `GET /api/v1/players;jsessionid=x` (rejected by Spring Security's StrictHttpFirewall, answered by `/error`) | 400 problem+json, request not echoed | `HTTP/1.1 400`, `Content-Type: application/problem+json`, body `{"detail":"That request was not valid. Check it and try again.","instance":"/error","status":400,"title":"Bad Request"}` |
| `POST /players`, a new player (zoe, invite code `testworld`), log in, then `POST /tables` (`Zoe live`, blinds 5/10, 6 seats) | 201 | 201, `open: true`, `seats: []`, `hostId` zoe's id |
| `POST /tables/{id}/close` by that host, empty table | 200, `open: false` | 200, `open: false`, `seats: []` |
| `POST /tables/{id}/seats/me/cash-out` with `{"points":1234.5}` | 400 problem naming the field, not the value | 400 `application/problem+json`, "points: use a whole number of points, in steps of 5" |
| 20 wrong invite codes from one `CF-Connecting-IP`, then the right code `testworld` | 403 x20, then 429 problem | 403 x20, then 429 `application/problem+json` "Too many wrong invite codes. Try again in 5 min."; the right code from another address: 201 |
| a short password and username on register | 400 naming the fields, not the values | 400 `application/problem+json`, "password: use 8 to 72 characters; username: use 3 to 32 letters, digits, '.', '_' or '-'" |

The app's log (a few lines at startup, nothing per request) holds no generated password, no token, no invite code and no password; its only warning is the testworld notice (the second pass, with `springdoc.api-docs.enabled: true` set, no longer logs springdoc's "`/v3/api-docs` is enabled by default"). The testworld's refusal to start on a database holding real players is covered by `TestworldGuardTest`, not shown live.

### Deviations from the plan
The plan is [PHASE_06_2026-10-11_BACKEND_FOUNDATION_TASKS.md](PHASE_06_2026-10-11_BACKEND_FOUNDATION_TASKS.md); its text is stale on these points.
- **The DB password has no fail-fast of its own.** The scope said "fail-fast on missing secrets (DB password, invite-code hash)". Only the invite-code hash stops the backend at startup. `application.yml` defaults `SPADE_DB_PASSWORD` to empty, so a missing password fails at Flyway's first connection, unless the local PostgreSQL trusts connections without one.
- **ArchUnit.** The public-view rule matches `Public*View` and `*PublicView` and nested types (the plan's `*PublicView` suffix would never have matched phase 07's `PublicTableView`). The testworld rule requires exactly `@Profile("testworld")` (the plan accepted any `@Profile`, including `"!testworld"`).
- **A mistyped invite-code hash never reaches the log** (nor does the plaintext code, if it is configured by mistake): the hex check moved from `@Pattern` into `SpadeProperties`' constructor.
- **`/error` answers problem+json too** (`ProblemErrorController`, hidden from the contract). Errors outside Spring MVC (security filters, the firewall, the container) were plain JSON.
- **Validation errors name the field and how to fix it** ("password: use 8 to 72 characters"), never the value. The plan's handler answered "Invalid request content." A body Jackson can't read does the same ("points: use a whole number of points, in steps of 5"); broken JSON gets "The request body isn't valid JSON.".
- **The database holds two invariants** (V1 and V2 edited in place; nothing was deployed): `player.username` must be lower case, and a host has at most one open table (a partial unique index). `buy_in` and `cash_out` carry no multiple-of-5 check on purpose: a later import may hold amounts that are not in chips.
- **One 401 sentence** from the security entry point and from `/error`: "Sign in first: this needs a valid session."
- **Orderings break timestamp ties by `id`** (open tables, a player's devices): under the frozen testworld clock every row ties.
- **The contract is fuller than the plan's.** Required request and response properties; the chip rule (`multipleOf: 5`, `maximum: 1000000`, minimum 5 for stakes and blinds, 0 for a cash-out); stable operation ids and tags by resource (`players`, `sessions`, `tables`); the username pattern anchored. `PointsRequest` split into `TopUpRequest` and `CashOutRequest` for that. `OpenApiConfig` refuses to build the spec when a key of its error map matches no operation.
- **A revoke can't be undone** by a request touching the same session at that moment (`@DynamicUpdate` on `DeviceSessionEntity`).
- **Login throttling.**
  - A failure that lands during a lockout no longer lifts it.
  - Each username locks after 5 failures, each caller address after 20 (`spade.login.max-failures-per-address`), so one typist at the table can't lock the group out: everyone shares the home's address through the tunnel.
  - Wrong invite codes are throttled per caller too (`AccountService.register` takes the caller's address).
- **Tables.**
  - Writes to one table are serialized by a row lock, so a top-up racing a cash-out can't resurrect a seat and a join racing a close can't seat someone at a closed table.
  - Every refused amount names the smallest chip.
  - Fractional amounts are refused instead of truncated (`spring.jackson.deserialization.accept-float-as-int: false`).
- **The testworld refuses to start or reset on a database holding real players** (`TestworldGuard`) and warns at startup. Its clock is frozen, so lockouts never expire until `POST /api/v1/testworld/reset`.
- **The contract** (`api/openapi.json`) describes bearer auth, problem+json error responses and JSON bodies, titled "Spade API" v1.

## What shipping it actually found
- **Each ArchUnit rule failed on its own planted violation.** None passed vacuously; the plants were deleted and the clean run is green.
  - `layers`: `Method <com.spade.api.PlantedApi.leak()> has return type <com.spade.repository.PlantedEntity>`.
  - `domainIsPure`: `Method <com.spade.domain.PlantedDomain.x()> calls method <org.springframework.util.StringUtils.hasText(java.lang.String)>`.
  - `apiNeverSeesEntities`: a class in `com.spade.api..` depending on an `@Entity` class.
  - `dtosAreRecords`: `Class <com.spade.api.dto.PlantedDto> is no record`.
  - `publicViewsNeverNameHoleCards`: failed on a `PublicTableView` with a `HoleCards` field and on `PlantedPublicView$Seat(HoleCards)` (`Field <com.spade.domain.PublicTableView.cards> has type <com.spade.domain.HoleCards>`). The first version (suffix `*PublicView` only) would not have guarded phase 07's `PublicTableView`.
  - `noFieldInjection`: `Field <com.spade.service.PlantedService.clock> is annotated with @Autowired`.
  - `noStandardStreams`: `Method <com.spade.service.PlantedService.shout()> gets field <java.lang.System.out>`.
  - `testworldOnlyUnderItsProfile`: failed on `@Profile("!testworld")` and on `@Profile({"testworld","prod"})`; passes on `@Profile("testworld")`. The first version only checked that some `@Profile` was present.
- ArchUnit's rules fail on an empty class set unless they carry `allowEmptyShould(true)`. Every rule whose package has classes now runs without it, so a renamed package fails the rule instead of passing silently. Only the public-view rule keeps it, until phase 07 adds a public view. **Re-prove a rule with a planted violation whenever a package is renamed.**
- **6 vacuous analyzer tests** in the predecessor's suite (`test_poker_hand_analyzer.py`) compare a tuple shape `get_best_hand` never returns. Not ported (Task 4).
- **1 impossible deal** in the predecessor's winner suite (`10S` dealt to two players). Corrected; the original is kept as a refusal test (Task 5).
- **The task reviews found defects in the plan's own code** (the deviations above). Each was proven by a test that failed first.
