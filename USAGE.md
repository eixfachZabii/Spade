# Usage

Commands only. Rules and reasons: [CLAUDE.md](CLAUDE.md).

## Backend (`spadeboot/`)
```bash
cp spadeboot/.env.example spadeboot/.env      # once; fill it in (openssl rand -base64 48 for SPADE_JWT_SECRET)
spadeboot/run.sh                              # dev profile: in-memory H2, seed users if SPADE_SEED_PASSWORD is set
(cd spadeboot && ./mvnw -q verify)            # tests
(cd spadeboot && docker compose up --build)   # Docker: known broken, see the Docker issue
```

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
