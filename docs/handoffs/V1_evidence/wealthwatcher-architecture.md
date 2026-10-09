# WealthWatcher's code architecture, mapped onto Spade V1

> **Evidence, 2026-10-09.** Item 3 for the V1 grill ([INDEX](../INDEX.md)). A read-only subagent audit (Opus) of WealthWatcher ("WW") at commit `f3345c60` (2026-10-06); the coordinator checked its Spade-side claims against the code. WW's process and tooling were audited in V0 ([tooling](../done/Version0.0/V0_audits/audit-wealthwatcher-tooling.md), [docs and process](../done/Version0.0/V0_audits/audit-wealthwatcher-docs-process.md)); this one covers the **code**. Every count holds at `f3345c60` only and comes with the command that re-derives it, run from the WW root.

## The short version
- **Layering:** WW retrofitted its layers in Phase 24 and then spent many phases cutting exemptions from 33 to 3. Spade gets the layer rules (ArchUnit) on its first commit.
- **Engine:** nothing in WW is a turn-based state machine. Only the shape carries over: state rebuilt by replaying events through a pure function.
- **Docs disagree with the code:**
  - WW's "pure" engines sit in `services/` and nothing checks their purity (5 of 19 import repositories).
  - Its frontend rules are prose only and already broken in places.
  - Its 2,540-line TypeScript type file is mirrored by hand from the backend schemas, with no check.
- **The test world** (a frozen, seeded backend that every e2e and snapshot check reads) is the part most worth copying. Spade's version should be invented data, not a copy of real data.
- **Adopt first:** layer rules, a pure hand engine, Java records, Flyway and a seeded test backend. **Next:** contract snapshots that double as decoding fixtures for the hub and the iPhone app. **Not in V1:** an i18n library or a visual-regression suite.

## Sources
**WW repo:** `/Users/sebastianrogg/PycharmProjects/Hackathons & Projekte/WealthWatchter`, commit `f3345c60`.

| Area | Paths read (relative to the WW root) |
|---|---|
| Backend | `server/.importlinter`, `server/pyproject.toml`, `server/uv.lock`, `server/app/main.py`, `server/app/core/{exceptions,exception_handlers,dependencies,config,owner_context,coalesce,data_world}.py`, `server/app/api/routes/{positions,watchlist}.py`, `server/app/api/schemas/{watchlist,summary}.py`, `server/app/repositories/financial_db.py`, `server/app/services/portfolio/portfolio_service.py`, imports of `server/app/domain/*.py` and `server/app/services/**/*_engine.py`, `server/data/fixture/MANIFEST.json` |
| Tests | `server/tests/{conftest,test_contract_snapshot,test_ai_tool_route_parity,test_routes_not_blocking,test_tax_year_golden_masters}.py`, `server/tests/__snapshots__/test_contract_snapshot.ambr` (size, git log), `scripts/{contract-diff.sh,dump_bodies.py}`, `.github/workflows/gate.yml` |
| Client | `client/{package.json,package-lock.json,eslint.config.js,tailwind.config.cjs,tsconfig.json,vitest.config.ts,playwright.config.ts,playwright.visual.config.ts}`, `client/src/api/{README.md,endpoints.config.ts,baseClient.ts,queryKeys.ts,endpoints/portfolio.ts}`, `client/src/app/{router,queryClient}.ts*`, `client/src/queries/{usePortfolioSummary,useToggleWatchlist}.ts`, `client/src/stores/createPersistedStore.ts`, `client/src/styles/theme.css`, `client/src/lib/{motion/presets,narrativePoll,reachability}.ts`, `client/src/routes/__root.tsx`, `client/src/i18n/config.ts` + `locales/`, `client/src/components/ui/Card.tsx`, `client/src/types/portfolio.ts`, `client/test/themeTokens.ts`, `client/e2e/helpers/testWorld.ts`, `client/e2e/{offline.spec.ts,visual/design-system.spec.ts}` |
| Docs | `CLAUDE.md` (animation rules), `DESIGN.md` (front matter), `docs/REPO_FACTS.md`, `docs/SEAMS.md`, `docs/backend/{README,ARCHITECTURE}.md` (layering, shared seam, tests), `docs/frontend/{README,ARCHITECTURE}.md` (data fetching, known debt), `docs/adr/0046-the-test-world-is-a-frozen-copy-of-the-real-one.md`, `.claude/skills/{add-surface,ship-phase}/SKILL.md` |
| Spade | [CONTEXT.md](../../../CONTEXT.md), [PRODUCT.md](../../../PRODUCT.md), [salvage.md](../../status-quo/salvage.md), [backend.md](../../status-quo/backend.md), [ADR 0002](../../adr/0002-physical-cards-camera-reads-manual-betting.md), [ADR 0005](../../adr/0005-rebuild-dont-repair.md), `spadeboot/pom.xml`, `spadeboot/src/main/resources/application.yml` |

**Locked versions.**
- **Server** (`uv.lock`): fastapi 0.138.0, pydantic 2.13.4, starlette 1.3.1, uvicorn 0.49.0, import-linter 2.11, syrupy 5.3.2, freezegun 1.5.5, pytest 9.1.0. **No ORM and no migration tool:** persistence is stdlib `sqlite3`.
- **Client** (`package-lock.json`): react 19.2.7, @tanstack/react-query 5.101.0, @tanstack/react-router 1.170.15, zustand 5.0.14, motion 12.40.0, i18next 26.3.1, tailwindcss 3.4.19, @playwright/test 1.60.0, vitest 3.2.7, typescript 5.9.3, vite = `rolldown-vite@7.2.5`, axios ^1.13.

**Scale** (why "don't copy the size" matters):

| What | Command | At `f3345c60` |
|---|---|---|
| Backend code lines | `find server/app -name '*.py' -not -path '*__pycache__*' \| xargs cat \| wc -l` | ~61k |
| Backend test lines | same over `server/tests` | ~66k |
| Client code lines | `find client/src -name '*.ts' -o -name '*.tsx' \| xargs cat \| wc -l` | ~47k |
| Markdown under `docs/` | `find docs -name '*.md' \| xargs cat \| wc -c` | ~3.0 MB |
| Backend `ARCHITECTURE.md` | `wc -c docs/backend/ARCHITECTURE.md` | 379 KB |

## 1. Backend layering

### 1.1 Layers and what each may import
The tree is `server/app/{api/{routes,schemas,middleware}, services/<group>/, repositories/, domain/, core/, scripts/}`. The rules live in `server/.importlinter` (abridged):

```ini
[importlinter:contract:layers]
type = layers
layers = api.routes / services / repositories        ; containers = app

[importlinter:contract:pure-leaf-types]
type = forbidden
source_modules = app.domain, app.api.schemas
forbidden_modules = app.services, app.api.routes, app.repositories

[importlinter:contract:routes-not-repos]
type = forbidden
source_modules = app.api.routes
forbidden_modules = app.repositories
ignore_imports = app.api.routes._utils -> app.repositories.fx_rates   ; + memory_db, watchlist_db (type-only Depends annotations)

[importlinter:contract:pure-concurrency-primitives]
type = forbidden
source_modules = app.core.coalesce, app.core.ai_pool
forbidden_modules = app.services, app.repositories, app.api
```

| Finding | Evidence |
|---|---|
| A `layers` contract lets a higher layer import **any** lower one, so routes→repositories needs its own `forbidden` contract. Exemptions went 33 → 12 → 4 → 3. | `.importlinter` comments |
| **`app.core` is in no contract.** The file admits "Five routes, three visible": `core/dependencies.py` re-exports repositories. | `.importlinter` "HONEST CAVEAT (Phase 45 audit)" |
| `domain/` is "pure" only as **no app logic**. It is built on pydantic, and two modules import `app.core`. | `grep -c BaseModel server/app/domain/*.py`; `grep -ln "from app.core" server/app/domain/*.py` → `gains.py`, `market.py` |
| **The pure computations ("engines") live in `services/`, not `domain/`.** Their purity is a naming convention: 5 of 19 `*_engine.py` import repositories. | `grep -lE "from app\.repositories" $(find server/app/services -name '*_engine.py') \| wc -l` |
| Services import response schemas, so the API boundary leaks downward (the contracts allow it). | `grep -rlE "from app\.api\.schemas" server/app/services \| wc -l` → 14 |
| **The shared seam.** Each read is assembled once by an `assemble_*` function that both the HTTP route and the AI tool call. `test_ai_tool_route_parity.py` asserts tool output == route output; it was written after two silent copies drifted. | `docs/REPO_FACTS.md` "Shared seams" (generated); the test's docstring |
| Composition root: `core/dependencies.py` provides singletons; routes take `Depends(get_*)`; tests use `app.dependency_overrides`. | `core/dependencies.py` docstring |

The layering was **retrofitted**. On 2026-06-19 the contract snapshot (`test(phase-24.0): freeze HTTP contract snapshot baseline`) and a starter contract landed first; the same day, Phases 24.2–24.3 moved modules into `repositories/` and `domain/` (`git log -- server/.importlinter`).

### 1.2 DTO and schema discipline
- **Requests:** pydantic models in `api/schemas/<domain>.py`, normalised at the boundary. Example: `WatchlistAddRequest._normalize` strips, upper-cases and refuses a blank `id`; the DELETE path parameter goes through the same `normalize_identifier`.
- **Responses are uneven.**
  - 52 of 69 route decorators declare `response_model=`.
  - The rest return dicts that services shape (`*_wire` functions); routes convert money by listing key names (`_POSITIONS_MONEY_KEYS` in `routes/positions.py`).
  - Derive with `grep -rhoE '@(router|app)\.(get|post|put|patch|delete)\(' server/app/api/routes server/app/main.py | wc -l` and `grep -rn -A3 -E '@router\.(get|post|put|patch|delete)\(' server/app/api/routes | grep -c response_model`.
- **Who maps:**
  - repositories return rows mapped into domain models;
  - services return plain dicts ("no Pydantic, no `Decimal`");
  - routes validate against the response model.
  - With no ORM there is no entity-vs-DTO problem.
- **Schemas carry the contract in comments.** Example: `PortfolioSummaryResponse.irr` deliberately has no default, so OpenAPI marks it required and the client shows `null` as "unmeasured", never 0.
- The wire format is snake_case (pydantic default).

### 1.3 Errors
- `core/exceptions.py` defines `WealthWatcherError(detail)` with `NotFoundError`, `BadRequestError` and `DataLoadError` below it.
- `core/exception_handlers.py` maps them once:

```python
_STATUS = {NotFoundError: 404, BadRequestError: 400, DataLoadError: 500}
status = _STATUS.get(type(exc), 500)                 # exact type: a subclass falls to 500
...
return JSONResponse(status_code=500, content={"detail": "Internal server error"})   # catch-all, no exception text
```

- The envelope `{"detail": str}` is FastAPI's, and 422 validation errors carry a **list** `detail`, so the envelope is not uniform.
- 6 route files still `raise HTTPException` (`grep -c "raise HTTPException" server/app/api/routes/*.py`).

### 1.4 Config
- `core/config.py` is a pydantic-settings `Settings` reading `.env` with `extra="ignore"`.
- Provider switches are `Literal`s: `PRICE_PROVIDER: Literal["mock","yahoo","fixture"]`.
- Model ids are `ClassVar` ("code, not configuration"); a leftover `.env` key is refused.
- Every store path is module-relative, so each git worktree gets its own `server/data/`.

### 1.5 Database access and migrations
- Raw `sqlite3`: one connection singleton per store per owner, behind a `threading.RLock`.
- Schema:
  - created with `CREATE TABLE IF NOT EXISTS`;
  - evolved by additive `ALTER TABLE … ADD COLUMN` **on the write path only** ("migrate on WRITE, tolerate on READ", `financial_db.py` ~L256);
  - why: the ledgers are committed binary `.db` files (one writer), and migrating on open dirtied them in every worktree.
- Schema-drift tests exist (`test_the_fixture_schema_is_the_real_schema`, `test_committed_dbs_are_not_wal.py`). No Alembic.

### 1.6 Background jobs and concurrency
- Route handlers are sync `def` on anyio's threadpool. `test_routes_not_blocking.py` fails on any `async def` handler outside an allowlist.
- `core/coalesce.single_flight(key, fn)`: one in-flight computation per key, and followers share the result. It has test seams, and a contract keeps it ignorant of what it coordinates.
- One daemon "warmer" thread per owner, started in `lifespan`.
- Small explicit latches (`ReachabilityGate`, `RefusalLatch`) with `note_failure`/`note_success` transitions.

### 1.7 Auth
**None.** Two people share one machine. `?owner=me|mom` becomes an ASGI middleware setting a `ContextVar`, with one SQLite file per owner. Nothing here maps to Spade's JWT.

## 2. The API contract snapshot
**What it freezes** (`server/tests/test_contract_snapshot.py`, syrupy, one `.ambr` of ~1.25 MB):
1. `test_openapi_schema_stable`: the whole `/openapi.json` minus AI paths and schemas.
2. `test_endpoint_bodies_stable` and `test_post_bodies_stable`: `{status_code, body}` for an explicit list of (path, params) pairs.
3. Determinism:
   - `freeze_time(FROZEN_NOW)`, where `FROZEN_NOW = data_world.fixture_epoch()`;
   - the frozen test world (`WW_DATA=fixture`);
   - `PRICE_PROVIDER=fixture`;
   - one module-scoped dependency override that restores the previous binding on teardown.
4. Two "proof-of-coverage" tests assert that frozen figures are not degenerate. Under mock prices they had been, so the snapshot froze numbers nothing exercised.

**Not frozen:** AI operations, every write, and one live third-party read; `docs/REPO_FACTS.md` generates that list.

**Updating:** `pytest --snapshot-update`, with written rules against doing it reflexively ("Never `--snapshot-update` to make red go green"; "the last step of an understood change").

**History** (`git log -- server/tests/__snapshots__/test_contract_snapshot.ambr`: 152 commits):
- It was born as the safety net for the Phase 24 layering refactor.
- The day after, a date-dependent refresh showed the clock was not frozen.
- The owner's weekly data imports moved it until ADR 0046 froze the test world, because re-baselining after each import "trains the reader to `--snapshot-update` reflexively".

**What it caught** (from code comments and ADRs; not re-run):
- it is the only gate on the benchmark ISIN map: a planted wrong fund leaves nine unit tests green and fails only the snapshot;
- the move to the test world exposed a fake profile stamped with the wall clock;
- a module-level override made one frozen body depend on pytest collection order.

**`scripts/contract-diff.sh` + `dump_bodies.py`** cover what the snapshot does not:
- They dump every **unfrozen** body at HEAD and at the merge-base (in a detached worktree), using the same data, fixture prices and a frozen clock.
- They scrub volatile keys, then `diff -ru`.
- Drift:
  - the header says 26 unfrozen operations, `REPO_FACTS.md` says 12;
  - `dump_bodies.py` hard-codes `FROZEN_NOW = "2026-06-23"` and never sets `WW_DATA`.

## 3. Frontend structure
| Concern | WW implementation | Enforced by |
|---|---|---|
| Endpoints | `src/api/endpoints.config.ts`: one `ENDPOINTS` object of strings and builder functions (`encodeURIComponent`) | prose only |
| HTTP client | `src/api/baseClient.ts`: one axios instance; interceptors stamp `?currency`/`?owner`, set long AI timeouts, attach a `normalized` error | — |
| Fetchers | `src/api/endpoints/portfolio.ts`: typed async functions | — |
| Query hooks | `src/queries/useX.ts`, one per resource, on TanStack Query v5. **Key factory** `src/api/queryKeys.ts`: every key starts `['portfolio', owner]`, because leaving the owner out of the key once showed two people's ledgers mixed on screen for ~4.5 s. Mutations write results with `setQueryData`. Global `QueryCache.onError` shows a debounced toast; `onlineManager` pauses queries while the backend is unreachable | `client/test/aiQueryKeys.spec.ts`, for one key set |
| Types | `src/types/portfolio.ts`, **hand-written** ("Mirrors `server/app/api/schemas/…`"), `wc -l` → 2,540. No OpenAPI generation, no parity check. A new endpoint takes 6 manual steps across both halves (`.claude/skills/add-surface/SKILL.md`) | nothing |
| UI primitives | `src/components/ui/`: 27 (generated list in `REPO_FACTS`), e.g. `Card` with padding and radius scales | prose + generated list |
| Design tokens | `src/styles/theme.css` custom properties (dark default + a light "paper" theme); `tailwind.config.cjs` maps names to `var(--…)`; a chart twin in `lib/chart/colors.ts`; `DESIGN.md` front matter is a third copy. `client/test/palette.spec.ts` parses `theme.css` and proves contrast; it exists because `colors.ts` and `theme.css` once drifted below 3:1 | vitest (properties); `DESIGN.md` unchecked |
| i18n | i18next + react-i18next + language detector; `locales/{en,de}/<namespace>.json`, 19 namespaces, ~260 KB of JSON (`find client/src/i18n/locales -name '*.json' -exec cat {} + \| wc -c`). en and de keys are symmetric today, but the test only checks that each namespace exists in both. e2e pins `locale: 'en-US'` | partial |
| Animation | `motion` v12 (`motion/react`); two presets in `lib/motion/presets.ts`; `<MotionConfig reducedMotion="user">` in `routes/__root.tsx`. CLAUDE.md rule: "Primary content must never use `initial={{ opacity: 0 }}`" | prose only |
| State | Zustand. `createPersistedStore` **requires** an explicit `version`, because an implicit 0 once silently threw saved records away | type-level |
| Routing | TanStack Router, code-based; loaders prefetch with `ensureQueryData` | — |
| Lint | ESLint flat config (`js`, `typescript-eslint`, react-hooks, react-refresh). **No import-boundary rule, no restricted imports, no i18n lint.** `tsc` strict | — |

**Rules not enforced.** "No API calls in components" (`docs/frontend/README.md`) is prose; 5 files under `components/` and `routes/` import fetchers directly (`grep -rlE "from '@/api/endpoints/" client/src/components client/src/pages client/src/routes`).

**Docs that disagree with the code:**

| Doc | Says | Code or generated facts say |
|---|---|---|
| `docs/frontend/README.md` | "20 namespaces", "61 tests in 11 spec files" | 19 namespaces and 31 specs (`docs/REPO_FACTS.md`) |
| `docs/backend/README.md` | "all 50 endpoints" | 63 paths / 69 operations (`docs/REPO_FACTS.md`) |
| `client/src/api/README.md` | `.js` files, "queryKeys.js (if used)" | TypeScript, and the key factory is central |

## 4. Tests
**Server** (`server/tests/`, flat; `ls server/tests/test_*.py | wc -l` → 216, plus `fixtures/` goldens and `__snapshots__/`):
- **`conftest.py`:**
  - forces `WW_DATA=fixture`;
  - defaults the providers to mock;
  - calls `app.openapi()` once before any `freeze_time`, because freezegun breaks lazy route resolution;
  - `@pytest.mark.real_data` swaps the stores onto **copies** of the real ones for one test.
- **Isolation:**
  - the test-world stores are copied once per process to a temp dir;
  - fakes are in-memory SQLite injected through `dependency_overrides` (60 files);
  - there is no factory library.
- **Golden masters:** tax-year tests use figures **transcribed from real documents**, never from app output ("read the diff as a bug report against the engine, never as a number to re-freeze").
- **Structural tests:**
  - routes-not-blocking;
  - an AST scan that only `repositories/` imports yfinance;
  - route/tool parity;
  - `test_repo_facts.py`, which regenerates `REPO_FACTS.md` and fails on drift.

**Client unit tests:** vitest over `client/test/*.spec.ts` in the node environment, on pure functions and the parsed stylesheet.

**The e2e "test world"** (ADR 0046, `server/app/core/data_world.py`):
1. `server/data/fixture/` holds a **committed, byte-faithful SQLite backup** of every store a frozen check reads.
   - It is cut at one **epoch**.
   - `MANIFEST.json` records the epoch, the source commit, a sha256 and row counts.
   - Only `scripts/build_data_fixture.py --as-of <date>` writes it; a test holds the files to the manifest.
2. One switch: `WW_DATA=real|fixture`, resolved only in `data_world.store_path()`. An unknown value raises.
3. The fixture is **never opened in place**: each store is copied once per process; import and seed scripts refuse to run under `fixture`.
4. Every frozen clock reads the epoch: `fixture_epoch()` on the server, `TEST_WORLD_EPOCH` in `client/e2e/helpers/testWorld.ts`.
5. `playwright.config.ts` starts **its own** backend (port 8100, fixture world, mock AI) and vite (3100), both with `reuseExistingServer: false`. Attaching to a sibling worktree's server had twice reported on the wrong code.
6. Outcome per the ADR: a simulated data import now moves 0 tests.

Spec style: 31 specs; 11 stub the network with `page.route`; most locate with `getByRole`. **In CI the e2e job runs only on manual `workflow_dispatch`.**

**Visual suite** (`playwright.visual.config.ts`): 4 routes × 2 themes = 8 PNGs, `threshold 0`. The baselines are pinned by `FIXTURE_FINGERPRINT` and `RENDERER` (the macOS build + the Chromium revision). Because it runs on macOS only, CI (Linux) never runs it.

## Mapping onto Spade V1

### (a) A slimmed Spring Boot backend
**Packages** (the root name is the grill's call; `com.spade` below):

| Package | Holds | WW counterpart |
|---|---|---|
| `api` | `@RestController`s and STOMP `@MessageMapping` controllers; `api.dto` request/response **records**; one `ApiExceptionHandler` | `api/routes` + `api/schemas` |
| `service` | `TableService`, `HandService`, `LedgerService`, `AuthService`: transactions, per-table serialisation, the assemblers that build views | `services/` (`assemble_*`) |
| `repository` | Spring Data interfaces + JPA `@Entity` classes + entity↔domain mapping; the outbound broadcaster adapter | `repositories/` |
| `domain` | `card` (Card, Rank, Suit: salvage "keep, adapt", no longer an entity), `eval` (HandEvaluator), `hand` (HandState, Street, Seat, HandEvent, Pots and side pots). **No Spring, no JPA, no Jackson.** | `domain/` + the `*_engine` modules WW left in `services/` |
| `config`, `security`, `core.concurrent` | wiring, JWT, per-table locks | `core/` |

**ArchUnit rules replacing the import-linter contracts.** Candidate code, **not compiled**; `archunit-junit5` 1.x assumed.

```java
@AnalyzeClasses(packages = "com.spade", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  // WW `layers` + `routes-not-repos` in one rule.
  @ArchTest static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
      .layer("Api").definedBy("..api..")
      .layer("Service").definedBy("..service..")
      .layer("Repository").definedBy("..repository..")
      .layer("Domain").definedBy("..domain..")
      .layer("Config").definedBy("..config..", "..security..")
      .whereLayer("Api").mayNotBeAccessedByAnyLayer()
      .whereLayer("Service").mayOnlyBeAccessedByLayers("Api", "Config")
      .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Config")
      .whereLayer("Domain").mayOnlyBeAccessedByLayers("Api", "Service", "Repository")
      .whereLayer("Config").mayNotBeAccessedByAnyLayer();   // no unconstrained `core` (WW's admitted gap)

  // WW `pure-leaf-types`, stricter: no framework either.
  @ArchTest static final ArchRule domainIsPure = noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
          "org.hibernate..", "com.fasterxml.jackson..", "jakarta.validation..",
          "..api..", "..service..", "..repository..");

  @ArchTest static final ArchRule noEntityInApi = noClasses().that().resideInAPackage("..api..")
      .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class);
  @ArchTest static final ArchRule dtosAreRecords = classes().that().resideInAPackage("..api.dto..")
      .should().beAssignableTo(Record.class);

  // PRODUCT.md: hole cards stay private until showdown. The public view cannot even name them.
  @ArchTest static final ArchRule publicViewHasNoHoleCards = noClasses()
      .that().haveSimpleNameEndingWith("PublicView")
      .should().dependOnClassesThat().haveSimpleName("HoleCards");

  // Status-quo debts (backend.md): field injection and System.out logging.
  @ArchTest static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
  @ArchTest static final ArchRule noStdout = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
```

Copy WW's habit of **proving each rule once by planting a violation**. That matters most for generic type arguments (`List<UserEntity>` inside a record).

**DTOs.**
- Every controller method returns a record. That closes WW's gap of 17 of 69 routes with no response model.
- Requests are records with Bean Validation (`@Valid @RequestBody`; `@Validated` on STOMP payloads).
- Mapping lives in `api` (`TableResponse.from(TableView)`). Services never return DTOs (WW's services import response schemas: the leak to avoid).
- **One assembler per view, used by both REST and STOMP:** `GET /api/tables/{id}` and the table-topic broadcast call the same `TablePublicViewAssembler`, and a parity test asserts the REST body equals the last broadcast. This is WW's `assemble_*` seam and its route/tool parity test, translated.
- One wire case everywhere (Jackson's camelCase default, or snake_case to match Swift's `.convertFromSnakeCase`).

**Errors.**
- One `@RestControllerAdvice` maps the domain exceptions (`IllegalMoveException`, `NotFoundException`, `NotYourTurnException`) to RFC 9457 `ProblemDetail`, built into Spring 6.
- The catch-all returns a generic message. That fixes the status quo's "raw exception messages reach clients; some errors return HTTP 200".
- STOMP: `@MessageExceptionHandler` + `@SendToUser("/queue/errors")` with the same body.
- One envelope for validation errors too.

**Config.**
- `@ConfigurationProperties` records with `@Validated`.
- Provider switches are enums (`spade.cards.source = CAMERA | MANUAL | SCRIPTED`), so an unknown value fails at startup.
- Keep the fail-fast JWT secret.
- Inject a `java.time.Clock` bean so tests can fix it (WW's frozen clock without freezegun).

**Migrations.**
- Flyway (`db/migration/V1__baseline.sql`) replaces `ddl-auto: update` (`application.yml` L14), with `ddl-auto: validate` so Hibernate checks the entities against the migrated schema at boot.
- Test: boot on an **empty** database and run every migration.
- The fidelity trade-off:
  - H2 in a compatibility mode is cheap and less faithful;
  - Testcontainers on the production database is faithful but needs Docker, which the gate does not cover;
  - the database itself depends on hosting (#8).
- Do **not** copy WW's migrate-on-write or committed `.db` files.

**The engine as a state machine.**
- **WW has nothing analogous**: no turn-based or event-driven state machine, and no concurrent writers.
- The nearest shape is state rebuilt by **replaying an append-only ledger through pure engines** (`portfolio_service.get_current_state()` → `ledger_engine.build_unified_ledger`). So the shape transfers, not the code: **a hand is a fold over its events.**

```java
// domain.hand: pure, sealed, unit-tested with no Spring
public sealed interface HandEvent permits HandStarted, HoleCardsScanned, BoardScanned,
                                          ScanCorrected, PlayerActed, HandAbandoned {}
public record HandState(Street street, List<Seat> seats, Board board, Pots pots, SeatId toAct /*…*/) {
  public HandState apply(HandEvent e) { /* or throw IllegalMoveException */ }
}
```

- **Entry:** everything ends in `HandService.submit(tableId, event, principal)`:
  - the player app's action → `PlayerActed`;
  - the player app's hole-card scan (Core ML on the phone, only the result is sent) → `HoleCardsScanned`;
  - the table camera, or **a person typing the board** → `BoardScanned`;
  - any correction → `ScanCorrected`. A correction is just another event, so "always possible" is structural.
- **Inside `submit`:**
  1. take the table's lock;
  2. load the hand, `apply`, append a `hand_events` row and update the projection in one transaction;
  3. **after commit**, broadcast the public view to the table topic and the per-player view to the player's private queue (`@TransactionalEventListener(AFTER_COMMIT)` is a candidate).
- **Status-quo defects this removes** ([backend.md](../../status-quo/backend.md)):
  - stale action responses (the response is built after `apply`);
  - a dead round thread killing the hand (there are no threads);
  - results never saved (same transaction);
  - events never sent (sent after every commit).
- Java 17 has sealed interfaces and records; pattern-matching `switch` is final only from Java 21 (a stack point). Spring Statemachine is a candidate only; a hand-written reducer is likely simpler.

**Golden masters.** WW's "transcribed from a real document, never re-frozen" becomes:
- the 18 evaluator vectors and the predecessor's 40 hand-analysis tests ([salvage](../../status-quo/salvage.md));
- side-pot cases worked out by hand;
- the ledger spreadsheet's zero-sum rule.

**Contract snapshot.**
- A JUnit test compares springdoc's `/v3/api-docs` to a committed `openapi.json` and updates only with an explicit flag.
- STOMP payloads are not in OpenAPI. Freeze them by **playing one scripted hand in the test world** and snapshotting every broadcast and private message, including an assertion that no public message carries hole cards before showdown.
- Scrub volatile keys.

**Test world.**
- A `testworld` profile: Flyway on a fresh database, a seed (named users, bankrolls, one table), `spade.cards.source=SCRIPTED` reading hands from versioned JSON built from the vectors, and a fixed `Clock`.
- Test-only endpoints sit behind `@Profile("testworld")`, guarded by an ArchUnit rule.
- **Synthetic, not a copy of real data:** WW copied real stores because real money carries audited properties; Spade's audited properties are the hand vectors.

### (b) The new web hub
| WW piece | In the hub | Simpler because |
|---|---|---|
| `ENDPOINTS` table | **yes**: one `endpoints.ts` | about a dozen endpoints |
| `baseClient` | **yes**: one fetch wrapper (auth header, error normalising) | no owner or currency stamping |
| Query hooks + key factory | **yes**: TanStack Query, `queryKeys.table(tableId).view`; **the table id in every key** (WW's owner-in-key lesson) | a handful of queries |
| Push | WW has none (polling; SSE only for chat). Realtime messages go into the cache with `queryClient.setQueryData(...)`; the REST GET is the first load and the resync after reconnect | WW's `setQueryData`-after-mutation is the nearest precedent |
| Reachability + status strip (ADR 0041) | **yes**: one strip that says why ("reconnecting since 21:14", "table camera offline") | — |
| `ui/` primitives | **a small set**: PlayingCard, Seat, Stack, Pot, Announcement, StatusStrip | no tables or forms |
| Tokens | **yes**: CSS custom properties in one file, plus a vitest that parses it and proves contrast. The style comes from [webapp-style.md](../../references/webapp-style.md), not WW | probably one (dark) theme |
| Motion | **yes**: one presets file, reduced motion, "never `opacity: 0` on primary content" (keeps headless tests and screenshots honest) | one signature moment: the board flip |
| i18n | **no library** (see below) | — |
| Persisted stores | at most one tiny settings store | — |
| Types | generated, or hand-written and checked against the snapshot fixtures | — |
| e2e test world | **yes**: Playwright `webServer` starts the backend in `testworld` on its own port, plus vite, no reuse | — |
| Visual suite | not in V1 | — |

**What i18n costs** in WW:
- 38 locale files and ~260 KB of JSON;
- every string through `t('ns:key')`;
- e2e pinned to `en-US`;
- a parity test;
- the language as an extra cache-key dimension.

A German/English friend group whose poker words are English anyway (fold, call, raise, all-in) needs **one** UI language, with the copy in one module; a later retrofit is mechanical. On iOS, String Catalogs make it almost free.

### (c) The SwiftUI player app
| WW idea | Swift translation | Transfers? |
|---|---|---|
| `ENDPOINTS` table | `enum Endpoint` with `path`/`method`; one `APIClient` (URLSession async/await, one decoder config) | yes |
| Hand-mirrored TS types | `Codable` models, hand-written and **decoded against the backend's committed snapshot JSON** in unit tests, or generated (swift-openapi-generator, candidate) | yes, with the check WW lacks |
| Query hooks + cache | `@Observable` stores per screen (`TableStore`, `HandStore`) that load, subscribe and apply pushes; injected via `.environment` | the idea |
| Error normaliser + status strip | one `APIError` from `ProblemDetail`; a "reconnecting" banner | yes |
| Tokens | asset-catalog Color Sets + a `Theme` enum for spacing, radius, durations | yes |
| `ui/` primitives | a `DesignSystem` folder: `PlayingCardView`, `StackView`, `ActionButton` | yes |
| Motion rule | `accessibilityReduceMotion`; same "never hide primary content" | yes |
| i18n | String Catalogs (`Localizable.xcstrings`) | free |
| e2e test world | XCUITest launched with the API URL pointing at a backend the gate starts in `testworld`; the camera behind a `CardScanner` protocol (`CameraScanner`, `ScriptedScanner`), since the Simulator has no camera | yes, adapted |
| Visual suite | snapshot tests pinned to a simulator + OS | later, if ever |
| Versioned persisted stores | if anything is persisted, version it explicitly | the lesson |
| TanStack Router, axios interceptors, owner/currency plumbing | — | no |

## What not to copy
| WW thing | Why not |
|---|---|
| Docs at WW's scale (72 KB CLAUDE.md, 379 KB backend ARCHITECTURE, 194 KB frontend, 82 KB SEAMS, 54 KB DESIGN) | Larger than the code they describe, and drifting: the disagreements above came from one read. Spade's ~5 KB CLAUDE.md budget (`doc-budget.json`) is right |
| 40-line history docstrings in code | Reasons belong in ADRs and commits |
| Raw SQLite, migrate-on-write, committed `.db` files, one-writer rule | Single-user and finance-specific. Spade is a multi-client server: Flyway |
| `?owner=` + `ContextVar`, no auth | Spade has a JWT per player |
| Engines in `services/`, pure by naming only | 5 of 19 import repositories already. Spade's engine goes in `domain/`, where ArchUnit can enforce it |
| An unconstrained `core` | WW's own config admits the gap. Every package belongs to a layer |
| Responses without a model; services importing response schemas | Records everywhere; mapping in `api` |
| 2,540 lines of hand-mirrored TS types, 6 manual steps per endpoint, no check | Generate, or check against snapshot fixtures |
| Frontend "hard rules" in prose | Five files break one already. Enforce with `no-restricted-imports`, or drop the rule |
| AI layer, warmers, fault injection, reachability latches, three price providers, FX, privacy mode | WW-specific. Spade's only input switch is the card source |
| A 1.25 MB snapshot, `contract-diff.sh`, generated `REPO_FACTS` from day one | Sized for 69 operations and money paths. Revisit when a count drifts |
| A macOS-pinned pixel suite | High upkeep (every macOS or Playwright update reds it); a few hub screens don't earn it in V1 |
| A list-shaped 422 next to `{"detail": str}` | One `ProblemDetail` shape |

## Recommendation
Adopt, in this order:
1. **Package layers + the ArchUnit test from the first commit**, covering:
   - layers and domain purity;
   - no entity in `api`, records in `api.dto`;
   - the `PublicView`/`HoleCards` privacy rule;
   - the field-injection and stdout bans.
2. **A pure `domain.hand` engine as a fold over `HandEvent`s**:
   - fed by one `HandService.submit`;
   - serialised per table;
   - broadcast after commit;
   - golden-master vectors (evaluator, side pots) as its spec.
3. **Records + Bean Validation + one `ProblemDetail` envelope** for REST and STOMP, plus one assembler per view shared by both, with a parity test.
4. **Flyway + `ddl-auto: validate`**, with a migrate-from-empty test.
5. **A `testworld` profile** (seed, scripted card source, fixed `Clock`), used by backend integration tests, the hub's Playwright `webServer` and XCUITest.
6. **Contract snapshots** of the OpenAPI JSON and one scripted hand's realtime payloads, updated only by a flag. **The snapshot bodies double as decoding fixtures** for the hub (vitest) and the iOS app (Swift Testing). That catches the drift WW's hand-mirrored types never could.
7. **Hub:**
   - endpoints table, one client;
   - TanStack Query with table-scoped keys, realtime messages written into the cache;
   - a small `ui/` set;
   - CSS-variable tokens with a contrast test;
   - motion presets plus reduced motion;
   - a status strip;
   - one language, no i18n library.
8. **iOS:** an `Endpoint` enum + `APIClient`, `@Observable` stores, asset-catalog colours, String Catalogs, a `CardScanner` protocol for tests.

**Alternative, lighter:** items 1–4 only, with hand-written clients and no snapshot or shared test world (each client mocks the backend).
- *Gain:* the least setup before the first hand runs.
- *Cost:* contract drift between three codebases surfaces at the table on a poker night, and nothing beyond the ArchUnit rule guards the privacy of realtime payloads.

**Alternative, heavier:** OpenAPI as the source of truth, generating the TS client (openapi-typescript/orval) and the Swift client (swift-openapi-generator), plus AsyncAPI for the realtime channel (Springwolf) and a hub visual suite.
- *Gain:* no hand-mirrored types at all.
- *Cost:* generator configuration on three toolchains, weaker realtime tooling, and a bigger gate on macOS runners.
- A middle path: generated **types only** for the hub, hand-written Swift checked against the snapshot fixtures (item 6).

## Not proven
- **None of WW's checks were run:** not pytest, `lint-imports`, vitest, Playwright, the visual suite, `contract-diff.sh` or its gate. What the snapshot "caught" comes from code comments, ADRs and commit subjects.
- `docs/backend/ARCHITECTURE.md` and `docs/frontend/ARCHITECTURE.md` were read only in the sections cited; other sections may disagree with the code in ways not listed here.
- The counts are `grep`/`find` heuristics at `f3345c60` (e.g. the `response_model` count looks 3 lines below each decorator). Re-derive before quoting.
- The en/de key parity is a one-off script, not a WW test.
- **The ArchUnit, Spring (`ProblemDetail`, `@TransactionalEventListener`, `@MessageExceptionHandler`), springdoc and Flyway snippets were written from memory and not compiled.** Method names and whether ArchUnit tracks generic type arguments must be checked against current docs in the first backend phase.
- Every library named in the mapping is a candidate; the stack decision is the grill's.
