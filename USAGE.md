# Usage

Commands only. Rules and reasons: [CLAUDE.md](CLAUDE.md).

## Backend (`backend/`)
```bash
(cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld)   # throwaway DB (Docker); players ana…fay, password testworld
cp backend/.env.example backend/.env && backend/run.sh                             # against a real PostgreSQL
(cd backend && ./mvnw -q verify)                                                    # tests (Docker must run)
(cd backend && SPADE_CONTRACT_UPDATE=1 ./mvnw -q test -Dtest=ContractTest)         # after a deliberate API change; commit api/
```
The testworld's clock is frozen, so a login lockout never expires until `POST /api/v1/testworld/reset`. Its invite code is `testworld`.
The legacy `spadeboot/` is frozen; its commands are in git history.

## Card detection (`cv/`)
```bash
git lfs install && git lfs pull               # once per clone; the model is in LFS
(cd cv && uv sync)                            # first run downloads torch
(cd cv && uv run python app.py)               # socket.io on :5001; opens camera 0
(cd cv && uv run pytest -q)
```

## Legacy apps (frozen; reference only)
Their dev certificates are untracked. Make them once:
```bash
for app in client webapp; do openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=localhost" -keyout $app/key.pem -out $app/cert.pem; done
(cd client && npm install && ./run.sh)        # hub, https://localhost:3000
(cd webapp && npm install && ./run.sh)        # phone app; reachable on the LAN
```

## Gate and CI
```bash
scripts/gate.sh [--backend|--cv|--docs]
gh run list --repo eixfachZabii/Spade --limit 5
```

## Backlog and board
```bash
gh issue list --repo eixfachZabii/Spade --label needs-grill
python3 scripts/check_board.py [--fix]
```

## Skills
| When | Skill |
|---|---|
| an idea, a bug, a "wouldn't it be nice" | `capture-idea` |
| before saying something works | `verify-change` |
| "we are done", "ship it", "push" | `ship-phase` |
| before building a version or a big phase | `/grill-with-docs` |
| turning a grilled design into tasks | `superpowers:writing-plans` |
