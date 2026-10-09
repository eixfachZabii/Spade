---
name: ship-phase
description: Use when a Spade phase or fix branch is finished and ready to integrate — "we are done", "push", "ship it", "merge this", or before any merge to master. Runs the gate, truths up the docs against the code, archives the phase docs, tidies the board, merges and pushes. Replaces the owner's global "we are done" checklist in this repo.
---

# Ship a phase

Work left on a branch is not shipped, and a doc that describes the plan instead of the code is a trap for the next session. This is the whole close-out, in order. (The owner's global checklist mentions ROADMAP files; Spade has none. See ADR 0003.)

## 1. Gate, from inside the worktree
```bash
scripts/gate.sh
```
Green is necessary, not sufficient. Read the **not proven** block and act on it:
- **Dirty tree?** The gate proved the worktree, not the branch. Commit or delete, then re-run.
- **Touched the engine, a game DTO, `/api/games/**` or a STOMP topic?** Run the `verify-change` privacy check.
- **Touched `cv/`?** Accuracy isn't measured. Measure it, or say so in the phase doc's "Not proven".

## 2. Truth up the docs against the code
Check each claim against the code at HEAD, never against the phase doc (plans go stale mid-phase):
- `docs/status-quo/` is a dated snapshot: don't rewrite it. If this phase made one of its statements untrue, add a line to that file's **Changed since** box.
- `CLAUDE.md`: new rules, commands, paths; never a count. Then `python3 scripts/doc_budget.py check`. If it grew on purpose, `python3 scripts/doc_budget.py update` and say why in the commit.
- `CONTEXT.md` for new or changed terms. `PRODUCT.md` only if a principle changed, which needs the owner.
- A decision that outlives the phase gets a new ADR: `docs/adr/NNNN-claim-as-title.md`, next free number.
- The phase doc: status `✅ shipped YYYY-MM-DD`, plus "What shipped, against what was scoped", "Not proven by this phase" and "What shipping it actually found".

## 3. Mark it in INDEX
In `docs/handoffs/INDEX.md`, the phase entry becomes `✅ merged YYYY-MM-DD — <outcome>` (add the merge sha in the next phase or the closeout, once it exists). If it was the version's last phase, the heading becomes `## ✅ Version N — shipped YYYY-MM-DD`. Never create a ROADMAP.md.

## 4. Archive the phase docs, all of them
```bash
mkdir -p docs/handoffs/done/VersionN.0
git mv docs/handoffs/PHASE_NN_* docs/handoffs/done/VersionN.0/
```
Include the tasks/plan doc and any audits folder. Then fix links **in both directions**:
- links *inside* the moved files gain one `../` per level (`../adr/` → `../../../adr/`);
- every link *to* them (INDEX, other phase docs, ADRs, status-quo) gets the new path.
```bash
python3 scripts/check_doc_links.py        # must print ✓
```
At a version's end also write `done/VersionN.0/CLOSEOUT.md`: what it found · what moved that the owner can see · what left the codebase · deferred, with reasons and issue links · process notes worth keeping.

## 5. Commit, merge, push
```bash
git status --short                          # nothing unexpected; never .env, *.pem, gurobi.lic
git add <the files you mean> && git commit -m "docs(handoffs): ship Phase NN — <outcome> (Phase NN)"
python3 scripts/check_commit_refs.py        # no negated close keyword
cd /Users/sebastianrogg/IdeaProjects/SPADE  # the main checkout, on master
git merge --no-ff phase/NN-slug -m "Merge phase/NN-slug — <one-sentence outcome> (Phase NN)"
scripts/gate.sh                             # again, on master
git push origin master                      # tell the owner you are pushing
```

## 6. Tidy the board
- Close every issue the phase completed, now that the merge sha is real:
  `gh issue close N --repo eixfachZabii/Spade --reason completed --comment "Shipped in Version N Phase NN (merge <sha>). <what changed>. Guarded by: <tests>."`
- File what you found and are not fixing, with `capture-idea`.
- `python3 scripts/check_board.py --fix`, then `python3 scripts/check_board.py` prints ✓.

## 7. Leave nothing behind
```bash
git worktree remove .worktrees/phase-NN-slug && git branch -d phase/NN-slug
git push origin --delete phase/NN-slug 2>/dev/null || true
git worktree list && git status --short      # one worktree, clean tree
```
`git branch -d` refuses when the branch's remote copy is behind; check `git merge-base --is-ancestor phase/NN-slug master`, then `-D` is safe.

## Never
- **`git stash`**: it is shared by every worktree.
- **Commit a secret**: `git diff --cached --name-only | grep -E '(^|/)\.env$|\.pem$|gurobi'` must print nothing.
- **"not fixing #N"** in a message: it closes #N. Write "Deferred: #N".
- **Leave a closed issue in Building**: run `check_board.py --fix`.
