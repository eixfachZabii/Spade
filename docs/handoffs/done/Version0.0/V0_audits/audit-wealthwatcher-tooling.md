> **Raw audit, 2026-10-09.** Read-only subagent report, kept as evidence for [the V0 spec](../V0_2026-10-09_FOUNDATION.md). Not maintained: Phase 04 turns it into `docs/status-quo/`. Paths and line numbers were true on the audit date.

I made no changes: nothing was edited, committed or filed, and the only GitHub calls were reads. The tooling in `WealthWatchter` boils down to one pattern: **whenever a rule written in prose got broken, they turned it into a script or test that the gate runs.** Every script header explains the incident behind it. That pattern is the thing worth copying to SpadeBoot, more than any single file.

All paths below are relative to `/Users/sebastianrogg/PycharmProjects/Hackathons & Projekte/WealthWatchter/`.

---

## (A) Tooling inventory

| Path | Purpose | Trigger | Transplant? |
|---|---|---|---|
| `.claude/settings.json` (committed) | Shared hook wiring, with a long `$comment` array explaining each choice | Every Claude Code / Devin session | **yes** |
| `.claude/settings.local.json` (gitignored) | Personal permission allowlist (`Bash(npm run dev:*)`, `Bash(gh pr *)`, Playwright MCP tools). No hooks here. | Session | adapt (personal) |
| `.claude/hooks/guard-master.sh` | **PreToolUse** on `^(Edit\|Write\|MultiEdit\|NotebookEdit\|edit\|write\|notebook_edit)$`. Blocks edits under `server/app/`, `server/tests/`, `client/src/`, `client/e2e/` when on `master` in the main tree. A linked worktree (`.git` is a file) passes; docs pass. Bypass with `WW_ALLOW_MASTER_EDIT=1`. Exits 2 and writes the reason both to stderr (Claude) and as `{"decision":"block"}` JSON on stdout (Devin). | Before every file edit | **adapt** (path globs) |
| PostToolUse + Stop hooks (in `settings.json`) | Run `~/.claude/skills/impeccable/scripts/hook.mjs` (design anti-pattern detector). It does nothing if the skill isn't installed. Commands start with `cd "${CLAUDE_PROJECT_DIR:-...}"` so its cache doesn't scatter into subfolders. | After a UI edit / at session end | optional |
| `.claude/skills/*/SKILL.md` | 5 repo skills (section B) | Matched on the description | adapt |
| `.claude/worktrees/` | Empty; Claude Code's native worktree directory | — | no |
| `.agents/skills` → symlink to `../.claude/skills` | Exposes the same skills to Codex and other `.agents`-convention tools. Gitignored. | — | only if you use Codex |
| `.codex/hooks.json` + `.codex/hooks/guard-master.sh` (symlink) | Codex copy of the hook wiring, with absolute machine paths, so gitignored. An earlier *copy* of the guard lost a fix, hence the symlink. | Codex | no |
| `AGENTS.md`, `.github/copilot-instructions.md` | Short pointer files that say "read CLAUDE.md". `test_repo_facts.py::test_agent_instructions_have_one_source` fails if the Copilot file grows past ¼ of CLAUDE.md. | — | **yes** (pointer idea) |
| `.superpowers/sdd/PHASE_4x_*` | Empty scratch dirs left by the superpowers subagent-driven-development plugin. Ignored. | Plugin | no, not load-bearing |
| `.impeccable/` | Design-detector cache, `design.json` (extracted design system), `critique/*.md`. Only `config.json` is tracked: the shared waiver list (rule + glob + written reason). | `/impeccable`, hooks | no (only if you adopt impeccable) |
| `.playwright/screenshots/` | Dump folder for Playwright MCP screenshots (global CLAUDE.md rule). Ignored. | MCP | yes (just a gitignore line) |
| `.worktrees/` | Gitignored home for `git worktree add .worktrees/<feat>`. Currently empty. **This is load-bearing:** the guard hook and the ship-phase skill assume all work happens here. | Manual | yes, if you run parallel sessions |
| `scripts/gate.sh` | The single gate script (section C) | Manual / ship-phase | **yes** |
| `scripts/check_doc_links.py` | Checks every relative `.md` link resolves. Ignores code spans and fences. | gate + CI | **yes, verbatim** |
| `scripts/doc_budget.py` + `doc-budget.json` | Byte ratchet on CLAUDE.md (`{"CLAUDE.md": 71953}`); `update` accepts a new size | gate + CI | **yes, verbatim** |
| `scripts/lint_ratchet.py` + `lint-baseline.json` | Baseline of lint violations fingerprinted as `file::code` (no line numbers) for ruff/pyright/eslint. Fails on a new violation. All three baselines are now `{}`. A linter that fails to run raises instead of counting as zero. | gate + CI | adapt (Checkstyle/SpotBugs/ESLint) |
| `scripts/check_commit_refs.py` | Flags a negated close-keyword in unpushed commits ("not fixed #18" still auto-closes #18) | gate | **yes, verbatim** |
| `scripts/check_scratch_dirs.py` | Tool scratch dirs (`.impeccable`, `.playwright`, `.claude`…) may only exist at the repo root; skips sibling worktrees | gate + CI | yes |
| `scripts/check_node_modules.py` | Installed react/react-dom must match the lockfile. Guards a `node_modules` symlinked into worktrees. | gate + CI | only if you share node_modules |
| `scripts/check_board.py` (40 KB) | Every open issue is on the board and in a column. Closed issues go to `Shipped`/`WontDo` by close reason. Issues "promoted" in `docs/handoffs/INDEX.md` stay open in `Ready`/`Building` with `type:phase`. `--fix` repairs. Field and option IDs are looked up at run time. | Manual / skills | **adapt (simplify heavily)** |
| `scripts/idea.sh` | `gh issue create --label type:idea,...` then `check_board.py --fix` | Manual | yes |
| `scripts/setup-project-board.sh` | One-off: create the user project and add every issue to it (idempotent) | Once | yes |
| `scripts/contract-diff.sh` + `dump_bodies.py` | Dumps the response bodies the snapshot test doesn't freeze, at HEAD and at the merge-base (in a detached worktree), then diffs them | Manual, when touching money paths | domain-specific |
| `db_status.py`, `build_data_fixture.py`, `build_price_fixture.py`, `audit_isin_assets.py` | Finance data and fixture tooling | — | no |
| `.github/workflows/gate.yml` | CI copy of the gate | push to any branch, PR, manual | **yes** |
| `.github/workflows/add-to-project.yml` | Adds new issues to the board and sets Status=Inbox | issue opened/reopened/transferred | yes |
| `docs/*/diagrams/*.puml` + `_ww-header.puml` | PlantUML sources with committed `.svg`/`.png`. Rendered by hand (`plantuml -tsvg *.puml`); **no script or CI renders them.** | Manual | optional |

There is **no** issue template, PR template, CODEOWNERS, labels-as-code, Makefile, pre-commit/husky config or Docker in this repo. Labels and the board were created by hand or with `gh`.

---

## (B) The five skills

Every skill opens with the incident that justified it, then gives numbered steps with bash blocks, then a **Never** list. All five are repo-specific: they name paths, ADRs and issue numbers.

**1. `ship-phase`** (241 lines)
```yaml
name: ship-phase
description: Use when a phase, workstream or feature branch is finished and ready to integrate — "we are done", "push", "ship it", "merge this", or before any merge to master. Runs the gate, truths up the docs, archives the handoff, fixes the links the archive breaks, and merges.
```
1. **Gate** from inside the worktree (`scripts/gate.sh [--full]`), then act on the "not proven" block: run contract-diff, stage untracked files, and never `--snapshot-update` blindly.
2. **Truth up the docs** against the code: `docs/{backend,frontend}/ARCHITECTURE.md`, CLAUDE.md (never add a count), `CONTEXT.md` glossary, a new ADR if warranted. Regenerate `REPO_FACTS.md`.
3. **Check the boxes:** flip the phase to ✅ in `docs/handoffs/INDEX.md`, the *only* roadmap (ADR 0032 retired `ROADMAP.md`). "Do not create a ROADMAP.md."
4. **Archive:** `git mv` *all* phase docs to `docs/handoffs/done/VersionN.0/`, fix links in both directions, run `check_doc_links.py`.
4b. **Tidy the backlog:** run `check_board.py` first; close completed issues with a comment ("Shipped in Phase N — module. doc path"); file what you found but aren't fixing; label `already-shipped`; then `check_board.py --fix`.
5. **Commit:** avoid negated close-keywords. Re-run the gate, `git merge --no-ff`, gate again on master, remove the worktree, delete the branch, push.
6. **Leave nothing behind:** `git worktree list`, `git status --porcelain`, `ls .worktrees/`.
- **Never:** `git stash` (it's repo-global across worktrees), restoring `chat_history.db` reflexively, `--snapshot-update` to make red go green.

**2. `verify-change`** (93 lines)
```yaml
name: verify-change
description: Use before claiming a change works, is fixed, or is complete in WealthWatcher — and before any commit that alters money, lots, prices, tax or a chart. Encodes the four verification traps…
```
The four traps:
1. A passing test may be pinning the bug. Revert the fix, watch the test fail, restore.
2. A number from the test harness isn't the number the app serves. Curl the running app.
3. The gate doesn't cover response bodies it doesn't freeze. Run `contract-diff.sh`. Pyright skips tests; missing i18n keys go uncaught.
4. A browser check needs the browser pointed at the right server. Assert `location.href`, warm the dev server first, save screenshots to `.playwright/screenshots/`.

It closes with: "State what you ran and what it printed."

**3. `add-surface`** (73 lines)
```yaml
name: add-surface
description: Use when adding a new API endpoint, page, route, chart, or asset (ISIN) to WealthWatcher — the wiring order across both stacks, and the check that comes before any of it.
```
- **Step 0: does it already exist?** Grep `services/` and `components/ui/`, and check CLAUDE.md's seam table. A route and an AI tool share one `assemble_*`.
- **Wiring order:**
  - Endpoint: router (thin) → service → repository (I/O) → `api/schemas` → `client/src/api/endpoints.config.ts` → fetcher → TypeScript type → `queries/useXxx.ts` hook → UI.
  - Page: `pages/X.tsx` → `routes/x.tsx` → `app/router.tsx` → `Sidebar.tsx`.
  - Chart: must go through `useChartBase`.
  - ISIN: run the live audit script, and never type `currency` by hand.

**4. `capture-idea`** (142 lines)
```yaml
name: capture-idea
description: Use when the owner has an idea, feature request, complaint or "wouldn't it be nice if" … Files it as a GitHub issue with the repo context attached, after checking whether it already exists.
```
1. **Check it exists:** `gh issue list --state all --search`, INDEX.md, grep services and components, REPO_FACTS. Three outcomes:
   - already shipped → file it and close it immediately as `already-shipped`;
   - partially there → file it open, saying which parts exist;
   - genuinely new → file it open.
2. **Body shape:** owner's words quoted, what already exists, constraints from CLAUDE.md, open questions.
3. **Label and board:** `gh issue create --label "area:…,type:idea,version:…"`, then **`check_board.py --fix` (required)**.
4. **Promotion rules:** INDEX.md is the roadmap and issues are the inbox. Promotion means a handoff doc plus an INDEX entry. The issue then becomes `type:phase`, sits in `Ready`, and stays open until the phase ships. One row per idea, never one per phase. A plain `#NN` is a citation; only a link inside a phase entry or a `**Builds:**` line counts as a promotion.

**5. `import-source-document`** (103 lines). This one is finance and tax specific: archive under `docs/handoffs/source-material/<person>/` with a strict filename, extract locally with pymupdf (never an LLM), and pin every checkpoint as a golden-master test. **Not transplantable**, but the idea "a real artefact becomes a golden-master test" is.

**Global skills** referenced in `USAGE.md` (`/grilling`, `/grill-with-docs`, `/closing-a-version`, `/handoff`, `/impeccable`, `/graphify`) live in `~/.claude/skills`, not the repo. They are **already available** in SpadeBoot.

---

## (C) Gate and CI

**`scripts/gate.sh`** (modes: default ~40 s, `--full`, `--backend`). It collects failures and prints a GREEN/RED verdict at the end.
- **backend:**
  - `uv run pytest -q` excluding the contract snapshot;
  - the contract snapshot separately under `PRICE_PROVIDER=fixture`;
  - `lint-imports` (import-linter layer contracts: routes → services → repositories; domain and schemas import no logic; routes may not import repositories);
  - `lint_ratchet.py check ruff pyright`.
- **frontend:** `check_node_modules.py`, `lint_ratchet.py check eslint`, `tsc --noEmit`, vitest (`client/test/`).
  - `--full` adds `vite build`, Playwright e2e (it starts its own backend on :8100 and vite on :3100 against frozen fixture data) and macOS-only visual screenshot baselines.
- **docs:** `check_doc_links.py`, `check_scratch_dirs.py`, `doc_budget.py check`, `check_commit_refs.py`.
- **"not proven by this gate" block** (printed every run):
  - a dirty working tree means the gate proved the worktree, not the branch;
  - N response bodies aren't frozen (the number is read from REPO_FACTS);
  - "touched cost basis? run contract-diff.sh";
  - "filed an issue? the board does not know — run check_board.py --fix";
  - e2e and the build didn't run without `--full`;
  - visual baselines are macOS-only.

**`.github/workflows/gate.yml`** runs on push to `**`, on PRs and on `workflow_dispatch`, with a per-ref concurrency group that cancels older runs. Jobs:
- `backend`: uv with Python 3.13 pinned, `uv sync --frozen`, pytest, snapshot, lint-imports, ratchet, and a check that **tests didn't modify committed files under `server/data`**;
- `frontend`: Node 24, `npm ci`, node_modules check, eslint ratchet, tsc, vitest, vite build;
- `docs`: links, scratch dirs, doc budget;
- `e2e`: **`workflow_dispatch` only** ("a slow flaky job is an ignored job"); uploads the Playwright report on failure.

The commit-ref check is deliberately local-only, because by the time CI runs, GitHub has already closed the issue.

**`add-to-project.yml`**: a preflight checks that the `ADD_TO_PROJECT_PAT` secret can reach the user project (GITHUB_TOKEN can't), then `actions/add-to-project@v2.0.0`, then sets Status=Inbox **only if the item has no status** (so reopened issues keep their column).

---

## (D) Issues, labels, milestones, board

- **Milestones:** none. Versions are tracked with labels.
- **Labels:** GitHub defaults plus three families:
  - `area:` ai · tax · charts · planning · devex · docs
  - `type:` `idea` ("Captured, not yet scoped") · `phase` ("A scoped phase with a handoff doc")
  - `version:` 4.0 · 5.0 · 6.0 · 7.0 · 8.0 · 9.0 · later (the descriptions carry the version codename and status)
  - flags: `needs-grill` ("Must go through /grilling before building") · `already-shipped`
  - `bug`/`enhancement` are still used as kind.
- **Board:** user project #2 "WealthWatcher", one Board view, a single custom Status field: **Inbox → Grilling → Ready → Building → Shipped · WontDo**. Current counts: 12 Inbox, 83 Shipped, 6 WontDo. All built-in project workflows are disabled except "Auto-add sub-issues". Closed issues are filed into columns by `check_board.py --fix`, because the "Item closed" automation fired zero times in eight closes.
- **Issue → phase mapping:** issues are the inbox, `docs/handoffs/INDEX.md` is the roadmap. Versions contain numbered phases, and each phase has one handoff doc (`PHASE_62_2026-09-25_FOCUS_SURFACES.md`: steps table, "Deliberately not done", "Done when"). On ship, the docs move to `done/Version9.0/`, plus a `CLOSEOUT.md` per version. ADRs live in `docs/adr/NNNN-*.md` (Date / Status / Context / Decision / Consequences, amendments appended).
- **PRs:** essentially unused (3, all from June). Work merges locally with `--no-ff` from worktrees.

**Issue body skeleton** (taken from #102, #111 and #81):
```markdown
**Owner, <date> (<context>):** *"<verbatim quote>"*

**What exists.** <files/services/components it builds on — paths in backticks>

**The idea / What happens.** <behaviour, with measured evidence/output>

**Why it was not fixed in #NN / Why it is <X>, not <Y>.** <reasoning>

**Constraints.** <rules from CLAUDE.md/ADRs that must not be violated>

**Open questions / Options.** <bullets for the grill>

**Not this issue:** <adjacent issues, by URL>
```
The closing comment follows a set pattern: "Shipped in Version 9.0 Phase 63 (merge `7a52e5ea`). <what changed, figures>. Guards: <tests/specs>."

---

## (E) Generated facts

`docs/REPO_FACTS.md` begins with a `<!-- GENERATED FILE — do not edit by hand -->` header and is written by **a pytest test**, `server/tests/test_repo_facts.py`.
- `render()` computes counts from the code: OpenAPI paths and operations from `app.openapi()`; frozen vs unfrozen endpoints; routes taking `?currency=`; `assemble_*` seams; i18n namespaces and en/de parity; ADR count; `components/ui/*.tsx`; test-module and e2e-spec counts.
- `test_repo_facts_are_current` diffs the rendered output against the committed file and fails with a unified diff.
- `REGEN_REPO_FACTS=1 uv run pytest tests/test_repo_facts.py` rewrites it.
- Companion tests check that the counts add up and that the pointer files stay pointers.
- The surrounding rules are what make it work: "never restate a number from it", the gate reads one count back out of it, and CLAUDE.md is byte-ratcheted.
- Diagrams are not generated. They are hand-rendered PlantUML, with a README rule to grep the rendered SVGs for removed names.

---

## (F) What SpadeBoot would need to adapt

SpadeBoot today has a Maven Spring Boot 3.4.4 / Java 17 backend (JPA, MySQL, Security, WebSocket, Dockerfile plus compose), two CRA (`react-scripts`) apps (`client/`, `webapp/`), 2 test classes, no `.claude/`, no `.github/`, no issues, and 9 default labels.

- **`guard-master.sh`:** change the `case` globs to `*/spadeboot/src/*|*/client/src/*|*/webapp/src/*` and rename the env var. The hook is optional unless you run sessions in parallel.
- **`gate.sh`:**
  - backend: `./mvnw -q verify`, or `test` plus Checkstyle/SpotBugs (or Spotless). ArchUnit replaces import-linter for controller→service→repository layering;
  - frontend, per app: `npm ci`, `CI=true npm test -- --watchAll=false`, `npm run build`; ESLint comes with CRA;
  - keep the docs block (links, doc budget, commit refs) and the "not proven" block.
- **`gate.yml`:** jobs for `actions/setup-java@v4` (temurin 17, Maven cache) running `./mvnw verify`, plus a matrix over `client`/`webapp` with setup-node and `npm ci`/test/build. MySQL either via Testcontainers or an H2 test profile.
- **REPO_FACTS equivalent:** a JUnit test that reads `RequestMappingHandlerMapping` (or springdoc `/v3/api-docs`) to list endpoints, counts `@Entity` classes and `@Service`s, renders markdown, and compares it to `docs/REPO_FACTS.md`. Regenerate with `-DregenRepoFacts=true`. Or a ~50-line Python script that greps annotations and runs in the gate.
- **`add-surface`:** rewrite the wiring order as Entity → Repository → Service → DTO (`api/dto/request`) → Controller → security config/WebSocket topic → client API call → component/route.
- **`ship-phase` / `capture-idea`:** keep the shape but strip the WealthWatcher incidents, IDs (`PVT_…`), ADR numbers and money traps. Your global CLAUDE.md "we are done" list is already the generic version. It still mentions `ROADMAP` files, which this repo retired in favour of `INDEX.md` + board; pick one model for SpadeBoot.
- **Labels:** `area:backend|client|webapp|infra|docs`, `type:idea|phase`, `needs-grill`, `version:X`.
- **Board:** run `setup-project-board.sh` with `TITLE=SpadeBoot`, then add the Status options (it creates only the project, not the columns).
- **Pointer files:** `AGENTS.md` / `copilot-instructions.md` pointing at CLAUDE.md.

## (G) What's overkill for a small project

- **Overkill:**
  - `check_board.py` as written (40 KB, INDEX-promotion parsing). Use `idea.sh` plus `add-to-project.yml`, or a 30-line "every open issue is on the board" check.
  - `contract-diff.sh`, fixture builders, `db_status.py`, ISIN audit, `import-source-document`.
  - `check_node_modules.py` and `check_scratch_dirs.py`, unless you symlink node_modules into worktrees or use impeccable.
  - Visual screenshot baselines.
  - The Devin/Codex dual wiring, `.agents/` and `.codex/` symlinks.
  - The full ADR/CONTEXT/DESIGN/PRODUCT/SEAMS doc set.
  - Version codenames with renumbering.
- **Cheap and high-value — take first:**
  1. `gate.sh` with a "not proven" block, plus `gate.yml`.
  2. Committed `.claude/settings.json`, and a `.gitignore` that uses `.claude/*` with `!` exceptions for the committed files.
  3. Three skills: ship-phase, verify-change, add-surface.
  4. `check_doc_links.py`, `doc_budget.py` and `check_commit_refs.py` copied as-is (they're generic Python).
  5. A short CLAUDE.md with no hand-typed counts, plus a tiny generated facts file.
  6. `area`/`type` labels, a 5-column board, the issue body skeleton above, and `capture-idea` reduced to "search first, then file, then board".
  7. A single `docs/handoffs/INDEX.md` roadmap, archived to `done/` per version.