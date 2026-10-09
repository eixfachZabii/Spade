#!/usr/bin/env bash
#
# PreToolUse guard: no code edits on `master` in the main working tree.
# Adapted from WealthWatcher's hook (2026-10-09). Docs and markdown stay allowed;
# code goes through a worktree per phase (CLAUDE.md, "Working in this repo").
#
# Escape hatch, for a deliberate one-liner:  SPADE_ALLOW_MASTER_EDIT=1
#
set -uo pipefail

[[ -n "${SPADE_ALLOW_MASTER_EDIT:-}" ]] && exit 0

payload="$(cat)"
path="$(printf '%s' "$payload" | python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))
except Exception: print("")' 2>/dev/null)"

[[ -z "$path" ]] && exit 0
[[ "$path" == *.md ]] && exit 0

case "$path" in
  *"/spadeboot/src/"*|*"/cv/"*|*"/client/src/"*|*"/webapp/src/"*|*"/scripts/"*) ;;
  *) exit 0 ;;
esac

repo_root="$(cd "$(dirname "$path")" 2>/dev/null && git rev-parse --show-toplevel 2>/dev/null)" || exit 0
[[ -z "$repo_root" ]] && exit 0
[[ -f "$repo_root/.git" ]] && exit 0          # a linked worktree has .git as a FILE

branch="$(git -C "$repo_root" rev-parse --abbrev-ref HEAD 2>/dev/null)"
[[ "$branch" != "master" ]] && exit 0

cat >&2 <<MSG
BLOCKED: code edit on 'master' in the main working tree.

  file: ${path#"$repo_root"/}

Make a worktree first:

  git worktree add .worktrees/<phase-or-fix> -b phase/<NN-slug>
  cd .worktrees/<phase-or-fix>

Then merge back with the ship-phase skill.
(Deliberate one-liner? Re-run with SPADE_ALLOW_MASTER_EDIT=1.)
MSG
exit 2
