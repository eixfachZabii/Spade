#!/usr/bin/env bash
# Starts the backend with the variables in spadeboot/.env (never committed).
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then
  echo "spadeboot/.env is missing: cp .env.example .env and fill it in (see USAGE.md)." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
source .env
set +a
exec ./mvnw spring-boot:run
