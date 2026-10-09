#!/usr/bin/env bash
#
# The gate. One command: backend, cv, docs, and then what it did NOT prove.
#
#   scripts/gate.sh             everything (first cv run downloads torch)
#   scripts/gate.sh --backend   spadeboot only
#   scripts/gate.sh --cv        cv only
#   scripts/gate.sh --docs      docs and devex checks only
#
# Shape and the "not proven" block copied from WealthWatcher's gate (2026-10-09).
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"

ONLY="${1:-all}"
case "$ONLY" in
  all|--backend|--cv|--docs) ;;
  --help|-h) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
  *) echo "unknown option: $ONLY (try --help)" >&2; exit 2 ;;
esac
want() { [[ "$ONLY" == all || "$ONLY" == "--$1" ]]; }

if [[ -t 1 ]]; then R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; D=$'\e[2m'; N=$'\e[0m'
else R=; G=; Y=; D=; N=; fi

FAILED=()
step() { printf '%s\n' "${D}── $1${N}"; }
ok()   { printf '  %s✓%s %s\n' "$G" "$N" "$1"; }
bad()  { printf '  %s✗%s %s\n' "$R" "$N" "$1"; FAILED+=("$1"); }
run()  { # run <label> <cmd...>
  local label="$1"; shift
  local out; out="$("$@" 2>&1)"; local rc=$?
  if [[ $rc -eq 0 ]]; then ok "$label"
  else bad "$label"; printf '%s\n' "$out" | tail -30 | sed 's/^/      /'; fi
}

printf '\n%sSpade gate%s  %s%s%s\n\n' "$Y" "$N" "$D" "$(git rev-parse --abbrev-ref HEAD) @ $(git rev-parse --short HEAD)" "$N"

if want backend; then
  step "backend"
  run "spadeboot: mvn verify" sh -c 'cd spadeboot && ./mvnw -q -B verify'
fi

if want cv; then
  step "cv"
  run "cv: uv sync (locked)" sh -c 'cd cv && uv sync --locked --quiet'
  run "cv: ruff" sh -c 'cd cv && uv run --locked ruff check .'
  run "cv: pytest" sh -c 'cd cv && uv run --locked pytest -q'
fi

if want docs; then
  step "docs & devex"
  run "relative links resolve" python3 scripts/check_doc_links.py
  run "CLAUDE.md within its byte budget" python3 scripts/doc_budget.py check
  run "no negated close-keyword in unpushed commits" python3 scripts/check_commit_refs.py
fi

step "not proven by this gate"
DIRT="$(git status --porcelain)"
if [[ -n "$DIRT" ]]; then
  printf '  %s!%s working tree is dirty: a green gate proves this working copy, not your branch\n' "$Y" "$N"
  printf '%s\n' "$DIRT" | head -12 | sed 's/^/      /'
else
  ok "working tree clean"
fi
printf '  %s· client/ and webapp/ (frozen legacy) are not built or tested%s\n' "$D" "$N"
printf '  %s· cv: only that the model loads and knows 52 labels; card-reading ACCURACY is not measured%s\n' "$D" "$N"
printf '  %s· no real phone, real camera or real poker night was involved%s\n' "$D" "$N"
printf '  %s· the Docker image was not built%s\n' "$D" "$N"
[[ "$ONLY" != all ]] && printf '  %s!%s partial run (%s): the other blocks did not run\n' "$Y" "$N" "$ONLY"

echo
if [[ ${#FAILED[@]} -eq 0 ]]; then
  printf '%sGATE GREEN%s\n' "$G" "$N"; exit 0
else
  printf '%sGATE RED%s: %d failed\n' "$R" "$N" "${#FAILED[@]}"
  printf '  - %s\n' "${FAILED[@]}"; exit 1
fi
