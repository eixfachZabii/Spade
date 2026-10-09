> **Raw audit, 2026-10-09.** Read-only subagent report, kept as evidence for [the V0 spec](../V0_2026-10-09_FOUNDATION.md). Not maintained: Phase 04 turns it into `docs/status-quo/`. Paths and line numbers were true on the audit date.

I've mapped WealthWatcher's documentation and planning system. Several of the brief's assumptions don't match the repo, so those corrections come first; they matter for what you copy into SpadeBoot.

**Corrections to the brief**
1. **There are no ROADMAP files any more.** ADR 0032 (`docs/adr/0032-the-board-owns-the-future.md`, 2026-08-25) deleted both `ROADMAP.md` files (174 KB). Its reason: 84% of their content duplicated phase docs, and 2 of their 19 open `- [ ]` bullets had already shipped. Today:
   - Future work lives on a GitHub project board.
   - Scoped phases live in `docs/handoffs/INDEX.md`, the only roadmap.
   - Each shipped thing lives in exactly one handoff doc.
   - The repo's `ship-phase` skill says outright: *"Do not create a `ROADMAP.md`, or a checklist of future work in any doc."* Step 3 of your global `~/.claude/CLAUDE.md` ("find the relevant ROADMAP files, mark `[x]`") is out of date for this repo.
2. **`source-material/{sebi,mom}` is evidence, not ideas.** It holds real payslips, tax assessments and broker exports that the tax engines are checked against as reference results ("golden masters"). Ideas travel a different path: owner remark → `capture-idea` skill → GitHub issue → board.
3. **"Handoff doc" here means the phase document** (design plus the record of what shipped). It is not the session-state `HANDOFF.md` that your global `/handoff` skill writes.
4. **Most of the process lives in skills, not docs.** The repo has its own (`.claude/skills/{ship-phase, capture-idea, verify-change, add-surface, import-source-document}`) and uses your global ones (`grill-with-docs`, `grilling`, `domain-modeling`, `closing-a-version`, `superpowers:writing-plans`). The docs are what those rituals produce.

---

## (A) Doc inventory

Paths are relative to `/Users/sebastianrogg/PycharmProjects/Hackathons & Projekte/WealthWatchter/`.

| File | Purpose | Audience | Generated? |
|---|---|---|---|
| `CLAUDE.md` (72 KB, 916 lines) | Agent rulebook: rules, a ~90-row "known shared seams" table, worktree/gate/board rituals, a "Do not" list | Agent; loaded into every session | Hand-written, but its size is capped by `scripts/doc_budget.py` (`doc-budget.json` = 71953 bytes). It must never contain counts. |
| `AGENTS.md` (16 lines) | Pointer only: "The instructions live in CLAUDE.md". `.github/copilot-instructions.md` is the same. Reason given: "Information redundancy is bad." | Codex, Copilot | Hand |
| `CONTEXT.md` (144 KB) | Domain glossary: `**Term** — definition … _Avoid_: synonyms`, sections tagged "(Phase N grilling — date)" | Both | Hand, written during grills; pruned at version close |
| `PRODUCT.md` | Users, jobs, positioning, constraints, brand commitments, principles. Carries the marker `<!-- impeccable:product-schema 1 -->` | Human + `/impeccable` | Hand; rewritten in V9.0 Phase 64 |
| `DESIGN.md` | YAML front matter of design tokens, then prose ("Creative North Star", Colors, Typography, Components, Do's and Don'ts) | Both | Hand, via `/impeccable` |
| `USAGE.md` | "Commands only. Rules and reasons: CLAUDE.md." Includes a skill table and backlog rules | Human | Hand |
| `README.md` | Public face | Human | Hand, and stale: its badge says v5.0 and its roadmap table says v6.0 is "Planned", while v9.0 has shipped |
| `docs/README.md` | Map of `docs/`, every row saying who reads it | Human | Hand |
| `docs/handoffs/INDEX.md` (99 KB) | The only roadmap, versions newest first | Human; also parsed by `scripts/check_board.py` | Hand |
| `docs/REPO_FACTS.md` | Every count: endpoints, ADRs, seams, `ui/` primitives | Both | **Generated.** Header: `<!-- GENERATED FILE — do not edit by hand. Produced by server/tests/test_repo_facts.py -->`; the test fails if it drifts |
| `docs/DECISIONS.md` | "Holding pen" table (Decision \| Rationale) for reasons not yet promoted to ADRs | Agent, on demand | Hand |
| `docs/SEAMS.md` (82 KB) | The "answer key" behind CLAUDE.md's seam table: CLAUDE.md is the trigger, this file is the why | Agent, on demand | Hand |
| `docs/adr/` | 51 ADRs, `NNNN-slug.md`; 0012 and 0023 are `-retired` tombstones | Both | Hand |
| `docs/{backend,frontend}/ARCHITECTURE.md` (379 KB / 194 KB) | Endpoints, response shapes, module map, stores | Human (the "start here" docs) | Hand |
| `docs/{backend,frontend}/diagrams/` | 14 + 8 **PlantUML** `.puml` files with committed `.svg` and `.png`, a shared `_ww-header.puml`, and a README with a "Regenerate" command | Human | Rendered from source |
| `docs/devex/SCRATCH_DIRS.md` | Which tool creates which dot-directory; moved out of CLAUDE.md | Agent | Hand |
| `docs/handoffs/done/VersionN.0/` | Archived phase docs plus `CLOSEOUT.md` | Both | Hand |
| `docs/handoffs/cancelled/` | Phase 46, with a "❌ CANCELLED — Do not implement from this document" banner | Both | Hand |
| `docs/handoffs/{source-material,deferred-source-material}/` | Evidence filed person-first, named `YYYY[-MM]_Issuer_Dokumenttyp.ext` | Engines/tests | Real documents |

---

## (B) Phase/version lifecycle, step by step

**Units of work**
- **Version:** a named theme with a one-sentence thesis ("9.0 · Focus", "8.0 · The App Waits").
- **Phase:** one numbered unit of work, numbered globally and in sequence. Numbers never reset across versions (1 → 64); V9.0 is phases 61–64.
- **Other INDEX categories:** "Shipped between versions — not phases" covers issue sweeps; "Ledger changes — not phases" covers data-only updates.
- **Decision IDs:** decisions are `D1…Dn`, process rules are `R1…Rn`. Commits cite them, e.g. `(Phase 62 step 4, D8)`.

**The lifecycle**
1. **Capture.** Any idea the owner mentions goes through `capture-idea`.
   - Before filing, check whether it already exists (INDEX, grep, issues).
   - File with the owner's words quoted, what already exists, the constraints, and the open questions.
   - Labels: `area:`, `type:idea`, `version:`, `needs-grill`.
   - Then run `check_board.py --fix`.
   - Board columns: **Inbox → Grilling → Ready → Building → Shipped · WontDo**.
2. **Claim a version.** INDEX gets a `🔲 Version N — queued · **Name**` section even before scoping, "so the number is claimed". The rule is "the scoped, grilled, buildable thing takes the number": China Style has been bumped 6.0 → 10.0 four times.
3. **Grill.** `/grill-with-docs` runs `grilling` and `domain-modeling` together.
   - For a whole version, parallel read-only audits run first and are committed as evidence (`done/Version9.0/PHASE_61_audits/audit-{backend,frontend,docs,features}.md`).
   - The grill record is written while the grill runs.
   - ADRs and `CONTEXT.md` terms are written the moment they are decided, not batched.
4. **Split into phase docs** named `PHASE_NN_YYYY-MM-DD_SLUG.md`.
   - INDEX gets one entry per phase.
   - Linking the issue *inside* that entry is what "promotes" it: the issue moves to `Ready` with `type:phase` and stays open until the phase ships.
5. **Plan (optional).** A large phase may get a `*_TASKS.md` written by `superpowers:writing-plans`. Only one exists (`done/Version4.0/PHASE_44_CONTRIBUTION_PLAN_TASKS.md`, 1797 lines). Phases from 53 onward put a short "Build order" or "tracks" table in the phase doc instead.
6. **Build.**
   - One worktree per phase or track (`.worktrees/<f>`, branch `feat/<f>`).
   - `.claude/hooks/guard-master.sh` blocks code edits on master.
   - Parallel tracks get disjoint file ownership and merge into one integration worktree.
   - Design and UX work goes one surface at a time with the owner in the browser, one commit per step.
7. **Record the outcome in the same doc.** The status line flips to `✅ shipped <date>`, and the doc adds "What shipped, against what was scoped", "Not proven by this phase", "What shipping it actually found", and even "Claims in this document that turned out to be wrong" (Phase 44 §11.3).
8. **`ship-phase`**, in this order:
   1. Run the gate and read its "not proven" block.
   2. Bring the docs up to date, checking each claim against the code.
   3. Regenerate REPO_FACTS.
   4. Flip the INDEX entry to `✅`.
   5. `git mv` every doc to `done/VersionN.0/`, fix links in both directions, and run `check_doc_links.py`.
   6. Tidy the board in all four directions.
   7. Commit without a negated closing keyword (enforced by `check_commit_refs.py`).
   8. `merge --no-ff`, re-run the gate, remove the worktree.
9. **Version closeout** via `closing-a-version` (audit → triage → fix → integrate → document). It produces `CLOSEOUT.md`. In V9.0 the docs pass was its own last phase (64), per rule R1: features → code → docs.

**Walkthrough: Version 9.0 "Focus"** (`docs/handoffs/done/Version9.0/`)
- **`PHASE_61_2026-09-25_FOCUS.md`** is the grill record:
  - the owner's brief, verbatim;
  - an Evidence table of four audits;
  - D1–D13 plus R1–R5 in a `# | Decision | By` table (By = owner or derived);
  - a verbatim "What the owner said about each surface" table;
  - a tracks table (`Track | Model | Owns | Findings`), with Opus/Sonnet chosen per track;
  - a "what moved" table (`Before | After | Why | Measured where`);
  - and even "Paused (usage limit) — resumed the same day".
- **`PHASE_62_…_FOCUS_SURFACES.md`** (35 lines) has a numbered steps table, "Deliberately not done (D10)" and "Done when".
- **`PHASE_63_…_MONEY_BACK.md`** has "The shape (decided)", "Rules that bind it" and "Done when".
- **`PHASE_64_…_DOCS.md`** is a doc-by-doc scope table with evidence links. It ends: "Every claim verified against the code, never transcribed from these plans."
- **`CLOSEOUT.md`** pulls the version together, below.
- **Links:** ADR 0051 is cited in every doc; CONTEXT terms (*Surface*, *Capability*, *Investing loop*, *Money back*) are named in headers; issues #99–#103 sit inside INDEX entries.

**Currently active:** nothing. Version 10.0 is "queued and not yet scoped — no phase numbers, no handoff docs". The handoffs root holds only `INDEX.md` and `STEUER_2026_PROGNOSE.md`, which describes itself as "Not a phase doc — a live working record".

---

## (C) Template skeletons (headings only)

**ADR**: `docs/adr/0041-…`, `0051-…`. ADR titles are stated as claims ("A feature leaves by verdict").
```
# NN. <Decision stated as a claim>
Date: YYYY-MM-DD
Status: Accepted [— amended YYYY-MM-DD]
Relates to / Supersedes / Closes / Amended by: <#issues, ADRs, CONTEXT terms>
## Context            (### sub-sections with measurements)
## Decision           (numbered D1… or 1.…)
## Alternatives considered
## Consequences
## Amendment — YYYY-MM-DD     (added at the end; earlier text is never rewritten)
```
- **Retired ADR:** `NNNN-retired.md` tombstone pointing to `git show <sha>:…`; the number is never reused.
- **Compare:** your global `domain-modeling/ADR-FORMAT.md` asks for only a title plus 1–3 sentences.

**Phase design/handoff doc** (Phases 58 and 59 are the mature form):
```
# Phase NN — <thesis title>
**Status:** ✅ shipped DATE · scoped and grilled DATE (`/grill-with-docs`, N decisions)
> What shipped, against what was scoped.
**Issue:** #  ·  **ADR:**  ·  **Glossary:** CONTEXT.md → terms  ·  **Sibling:** Phase
## 1. One sentence
## 2. What grilling measured that the issue did not know
## 3. Decisions               (| # | Decision |)
## 4. Build order             (waves / worktrees)
## 5. The guard (not just the correction)
## 6. Not proven by this phase
## 7. What shipping it actually found
```
Variants add "What this phase must not do", "Definition of done" and "Provenance".

**Version grill record:** Phase 61's structure above.

**Tasks/plan doc** (`superpowers:writing-plans`):
```
# Phase NN — <Name> · Implementation Plan
> For agentic workers: REQUIRED SUB-SKILL: superpowers:subagent-driven-development …
**Spec:** <phase doc>  **Goal:**  **Architecture:**  **Tech Stack:**
## Global Constraints   (worktree/branch, exact gate commands, revert-and-observe rule)
### Task N: <name>
**Files:** Create / Modify / Test   **Interfaces:** Consumes / Produces
- [ ] Step 1: … (commands + "Expected:")
## Self-Review          (spec coverage · type consistency · execution order)
```

**Version CLOSEOUT.md:**
```
# Version N · <Name> — closeout
(dates, base sha, owner brief verbatim, version in one sentence, links)
## What it found
## What the owner said, surface by surface
## What moved that the owner can see  (before → after)
## What left the codebase
## What was unified
## What was verified CLEAN
## Deferred, with reasons            (| Item | Why not now |, each with an issue)
## Process notes worth keeping
```

**INDEX.md:**
```
# Handoff Index   (rules: only roadmap; how a build target is linked)
## 🔲 Version N — queued · **Name**
## ✅ Version N — shipped DATE · **Name**
> Grilled DATE. N decisions · ADRs · Glossary terms
> **The version is one sentence:** …   > Deliberately deferred: #…
- **NN — Title** — ✅ merged DATE — outcome · [#issue] · [doc]
### ❌ Cancelled
## ✅ Shipped between versions — not phases
## 📒 Ledger changes — not phases
```

**CLAUDE.md** (top-level order):
- Pointer header to the ARCHITECTURE docs, PRODUCT and DESIGN
- Project at a glance
- **Before you write code — does it already exist?** (5 questions plus the seam table, `⚠` = trap, `…` = more in SEAMS.md)
- Where the work lives (roadmap / inbox / facts / commands)
- Working in this repo — ALWAYS use an isolated worktree
- Tool-generated scratch directories
- How to run
- **Changes — the required gate** (one command, plus "the three ways this gate lies to you")
- Backend structure & layering · tech stack & config
- API endpoints (a pointer only; no table, because copies drifted)
- The domain model section
- Frontend tech/structure · design system · layout / API-layer / i18n / animation / chart / formatting rules
- Key architectural decisions and why (5 bullets plus a pointer to DECISIONS.md)
- Common tasks (pointer to a skill)
- **Do not**

---

## (D) Conventions that carry real weight

1. **One home per fact; pointers, not copies.** AGENTS.md points at CLAUDE.md. CLAUDE.md has no endpoint table. SEAMS.md is the answer, CLAUDE.md only the trigger. ADRs are canonical and DECISIONS.md summarises them.
2. **Never write counts by hand.** Generate them (`REPO_FACTS.md` plus a test that fails on drift), or write how to derive them. Measured: "three of six hand-typed counts were stale one day after Phase 49."
3. **Each kind of work has one home.**
   - Future work → the board, because a row has a state.
   - Scoped work → INDEX.
   - Shipped work → one handoff, complete in itself (ADR 0032 D2: "No handoff may point outside itself for its own record").
4. **Grill before you build, and write it down as you go.** Every version and almost every phase names `/grill-with-docs`. The owner's words are quoted verbatim, and decisions get IDs that commits cite.
5. **Check claims against the code, not the plan.** "The plan is stale by now." Every doc carries "Not proven" and "what turned out wrong" sections.
6. **One gate command that admits its gaps.** `scripts/gate.sh` ends by printing a "not proven by this gate" block.
7. **Enforce rules with scripts, not prose** ("fix means build the guard"). Examples: `doc_budget.py`, `lint_ratchet.py`, `check_board.py`, `check_doc_links.py`, `check_commit_refs.py`, the `guard-master.sh` hook.
8. **Worktree per unit of work, enforced.** Never `git stash`. Merges are `--no-ff`. "Work left on a branch is not shipped."
9. **Archive with `git mv` and fix links both ways**; verify them, don't assume.
10. **Numbering:** phases are global and sequential; file names are dated; ADR numbers are never reused; ADRs get amendments added at the end and are superseded, never edited away.
11. **Commit format** is Conventional Commits (of 2096 commits: feat 497 · docs 471 · fix 346 · test 109 · refactor 108).
    - Subject: `type(scope): <outcome as a claim> (Phase N step M, D#, #issue)`.
    - Scopes are areas (`ai`, `tax`, `index`, `adr`, `claude`); early history used phase numbers as scopes (`phase-35`, `25.1`).
    - Other prefixes: `data:` for data-only commits, `wip(<track>): checkpoint` after session limits.
    - Merges: `Merge <branch> — <one-sentence outcome> (ADR NNNN)`.
12. **Branch names:** `feat/<issue#>-<slug>`, `fix/<issue#>-<slug>`, `docs/…`, `chore/…`, `data/…`, plus version-scoped ones like `v9/<track>`, `grill/v9-focus`, `integrate/open-issues-<date>`. All are deleted after merge; only `master` remains.
13. **The negated-keyword trap.** "Filed and not fixed: #18" still closes #18 on GitHub. Write "Deferred: #18" instead.

---

## (E) What SpadeBoot should not copy

- **Size.** Don't start with a 72 KB CLAUDE.md, an 82 KB SEAMS.md and a 33-row DECISIONS.md. They grew from incidents in the finance domain. Copy the *shape* (pointer header, "does it already exist?", one gate command, Do-not list) at roughly 5–10 KB.
- **A 144 KB CONTEXT.md.** It contradicts its own skill's rule that the glossary be "totally devoid of implementation details": many entries are full specs with formulas and module names. Keep the `**Term** — … _Avoid_:` format and keep entries short.
- **Huge ARCHITECTURE docs and 22 PlantUML diagrams** with committed SVG and PNG. One ARCHITECTURE.md per side with 1–3 Mermaid diagrams is enough.
- **Board machinery**: `check_board.py`, the promotion-parse rules (#71), and the WontDo amendment to ADR 0032. Plain Issues plus labels, and "close with a comment", cover a small project.
- **Domain-specific rituals**: the frozen contract snapshot and fixture world, `source-material` with personal data (its README carries a GDPR warning), `deferred-source-material`, "Ledger changes", version renumbering.
- **Concurrent-session defences** (node_modules symlink checks, scratch-dir cleaner, stash bans), unless you also run several agent sessions at once. The `guard-master.sh` hook is cheap and worth copying.
- **The heavy ADR format.** Use the minimal format from `domain-modeling` (title plus 1–3 sentences) and add sections only when needed.
- **Separate 1800-line TASKS docs.** Use them rarely; recent phases manage with an inline "Build order".
- **The cautionary tale: hand-maintained docs drift even here.**
  - `README.md` still says v5.0.
  - `capture-idea` still lists `version:7.0` as China Style, which is now 10.0.
  - CLAUDE.md's "one row per idea" rule needed three ADR amendments.
  - **Practical takeaway:** keep fewer docs and lean on the gate.

---

## (F) Process lessons from memory

Source: `~/.claude/projects/-Users-sebastianrogg-PycharmProjects-Hackathons---Projekte-WealthWatchter/memory/`

1. **`wants-subagent-workflow`.** Use parallel subagents, each in its own worktree: Sonnet for mechanical work, Opus for complex. Give each its own ports and pre-assign ADR numbers. About 4 concurrent agents (1–2 Opus) is sustainable; 13 burned the usage limit in about 10 minutes. Resume a cut-off agent with SendMessage after a WIP commit.
2. **`paced-review-gated-design-work`.** Design and UX are the exception: one surface at a time with the owner looking. Split repair from re-composition and gate each separately. Never run a design pass in parallel.
3. **`plans-go-stale-mid-phase`.** Subagents copy a stale plan faithfully. Put "Corrections to the brief — these override it" in each dispatch prompt. Closeout docs must cite code.
4. **`fix-means-build-the-guard`.** "Make sure it never happens again and document it so no AI ever does this mistake again." Ship the check together with the fix, and wire the check into the gate.
5. **`verify-tests-by-reverting-the-fix`.** Revert the fix, watch the test go red, restore it. Phase 41 found four tests that passed without their fix.
6. **`green-gate-proves-worktree-not-branch`, `compare-test-counts-at-merge`, `sibling-phase-merge-timing`.** Check `git status` inside verification. Test counts should never go down on a feature merge. Always run a final whole-branch review against current master.
7. **`check-the-running-server-before-claiming-done`.** Version 8.0 was declared shipped while the owner's live server was still misbehaving. Check the real running app.
8. **`grills-always-load-domain-modeling`, `grill-ask-only-taste`.** A grill means `grill-with-docs`. Answer every branch you can from the code, ADRs or a measurement, and show the answer. Only placement, look, wording and priority go to the owner, each with a recommended default.
9. **`judge-surfaces-by-his-usage`.** Leave alone what he uses and likes, even if an audit calls it redundant. Cut what he saw once and never used again. PRODUCT, DESIGN and CONTEXT can be reworked.
10. **`backlog-lives-in-github-issues`, `ask-the-owner-for-source-documents`.** Reconcile ideas before filing: 5 of 11 Discord ideas had already shipped. Ask for the document early; one question can remove a whole design branch.
11. **`master-gets-pushed-by-something-else`, `generated-counts-collide-across-branches`.** Treat a merge to master as publishing it, since `Closes #N` fires on push. Never resolve a conflict in a generated file by picking a side; regenerate it.
12. **`diagnose-cause-not-first-symptom`.** Trace the whole chain, then look one step past your own fix for the same class of bug.