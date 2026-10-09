#!/usr/bin/env bash
# Tests for guard-master.sh. Run from anywhere: .claude/hooks/test_guard_master.sh
set -uo pipefail
HOOK="$(cd "$(dirname "$0")" && pwd)/guard-master.sh"
MAIN="$(git -C "$(dirname "$0")" worktree list --porcelain | sed -n '1s/^worktree //p')"
fails=0
check() { # check <expected-exit> <description> <path> [env]
  local want="$1" desc="$2" path="$3"; shift 3
  printf '{"tool_input":{"file_path":"%s"}}' "$path" | env "$@" "$HOOK" >/dev/null 2>&1
  local got=$?
  if [[ $got -eq $want ]]; then echo "ok   $desc"; else echo "FAIL $desc (exit $got, want $want)"; fails=$((fails+1)); fi
}
BRANCH="$(git -C "$MAIN" rev-parse --abbrev-ref HEAD)"
if [[ "$BRANCH" == master ]]; then
  check 2 "backend code on master in main tree is blocked" "$MAIN/spadeboot/src/main/java/X.java"
  check 2 "cv code on master in main tree is blocked"      "$MAIN/cv/app.py"
  check 2 "legacy client code is blocked"                  "$MAIN/client/src/App.js"
  check 2 "a new file in a new backend package is blocked" "$MAIN/spadeboot/src/main/java/com/spadeboot/engine2/Foo.java"
  check 2 "a new cv folder is blocked"                     "$MAIN/cv/spike/read_board.py"
  check 0 "a markdown file under cv/ passes"               "$MAIN/cv/README.md"
  check 0 "docs pass"                                      "$MAIN/docs/handoffs/INDEX.md"
  check 0 "the bypass variable passes"                     "$MAIN/cv/app.py" SPADE_ALLOW_MASTER_EDIT=1
else
  echo "skip main-tree cases: main checkout is on '$BRANCH', not master"
fi
WT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
if [[ -f "$WT/.git" ]]; then
  check 0 "code inside a linked worktree passes" "$WT/spadeboot/src/main/java/X.java"
  check 0 "a new package inside a linked worktree passes" "$WT/spadeboot/src/main/java/com/spadeboot/engine2/Foo.java"
fi
check 0 "an empty payload passes" ""
exit $fails
