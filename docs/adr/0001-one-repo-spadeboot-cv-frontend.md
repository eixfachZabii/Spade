# 1. Spade is one repo: spadeboot, cv and the frontend live together

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/done/Version0.0/V0_2026-10-09_FOUNDATION.md) D4

## Context
Card detection lived in a separate repo (`lucabzt/spadeAI`), and its contract drifted from its only caller: the legacy webapp sends socket.io frames to the backend's port, so they never reach it. A feature that crosses services (a scanned card reaching the engine) needs one commit, one gate and one board.

## Decision
`cv/` (Python, uv) sits next to `spadeboot/` (Spring Boot) and the future frontend. It was imported as a snapshot of `spadeAI@9e4ec5e`, without history. The model weights are in Git LFS. One `scripts/gate.sh` and one CI workflow cover every service.

## Consequences
- A cross-service change lands atomically and is gated once.
- The repo needs git-lfs. CI pulls the model and caches it, because LFS bandwidth is metered.
- Python and Java tooling sit side by side; each service keeps its own build file.
