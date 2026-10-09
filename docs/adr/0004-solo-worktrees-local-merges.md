# 4. Solo with AI: one worktree per phase, local --no-ff merges, no pull requests

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D5, D18

## Context
One person builds Spade, with AI agents, sometimes several sessions at once. Pull requests would add ceremony without a second reviewer. Sessions sharing one working tree overwrite each other's files.

## Decision
- Each phase is built in `.worktrees/phase-NN-slug` on `phase/NN-slug`.
- It merges into `master` locally with `--no-ff` through the `ship-phase` skill, and the gate runs before and after the merge.
- `.claude/hooks/guard-master.sh` blocks code edits on master in the main checkout.
- Phase numbers are global and never reset. V0 is phases 01–05.

## Consequences
- History reads as one merge commit per phase.
- Review happens in the session (the `verify-change` skill and the gate), not on GitHub.
- If a second contributor joins, this ADR is superseded by one that introduces PRs.
