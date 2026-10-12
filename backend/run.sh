#!/usr/bin/env bash
# Runs the backend against the PostgreSQL in backend/.env.
# A throwaway database instead (Docker running):
#   (cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld)
set -euo pipefail
cd "$(dirname "$0")"
[[ -f .env ]] || { echo "backend/.env is missing: cp .env.example .env and fill it in" >&2; exit 1; }
set -a; . ./.env; set +a
exec ./mvnw -q spring-boot:run
