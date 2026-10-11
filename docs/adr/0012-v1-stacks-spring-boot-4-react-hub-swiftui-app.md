# 12. V1's stacks: Spring Boot 4.1 backend, React hub, SwiftUI app, one contract

Date: 2026-10-10
Status: Accepted
Relates to: [ADR 0005](0005-rebuild-dont-repair.md) (which left the stacks to V1) · [V1 evidence](../handoffs/V1_evidence/README.md) · V1 grill

## Context
ADR 0005 decided to rebuild but left the stacks and names to V1's research. The research is in [V1_evidence/](../handoffs/V1_evidence/README.md). Spring Boot 3.4 and 3.5, the 2025 backend's line, are out of open-source support (3.4 since 2025-12-31, 3.5 since 2026-06-30). Everyone in the group has an iPhone.

## Decision
**Backend: `backend/`, a new project.**
- Spring Boot 4.1 on Java 21.
- **Packages:**
  - `api` (controllers, request/response records);
  - `service`;
  - `repository` (JPA entities stay here);
  - a **pure `domain`** (cards, the evaluator, the hand engine: no Spring, JPA or Jackson);
  - `config` and `security`.
- **ArchUnit rules from the first commit:**
  - the layers;
  - domain purity;
  - no entity in `api`;
  - records in `api.dto`;
  - public views can't name hole cards;
  - no field injection;
  - no `System.out`.
- **Errors:** one `ProblemDetail` envelope for REST and the stream.
- **Persistence:** Flyway with `ddl-auto: validate`; PostgreSQL, tested via Testcontainers.
- **Contract:**
  - springdoc exports `openapi.json`, which is committed;
  - the gate fails on drift;
  - contract-snapshot bodies are decoded in both clients' tests;
  - a `testworld` profile (seed, scripted card source, fixed clock) drives backend, hub and app tests.
- **Salvage:** ported per the salvage map. Today's `spadeboot/` is frozen and deleted when V1 ships.

**Hub: `hub/`.**
- Vite 8, React 19, TypeScript 6.0, TanStack Query + Router, Tailwind 4, Motion, Vitest, Playwright.
- A static build served by the backend: one origin, one process.
- Cards are drawn, not the PNG deck: one large index, legible across the room.

**Player app: `ios/`.**
- SwiftUI, minimum iOS 26: a thin Xcode target plus a local Swift package with the logic.
- Observation, `NavigationStack`, Swift 6 strict concurrency.
- **Cards:** AVFoundation + Vision run the model exported to Core ML (FP16, embedded NMS), on the phone; only the labels leave it. Typed entry is always one tap away.
- Keychain for the session token.
- **Tests and CI:** Swift Testing + XCUITest; GitHub Actions macOS runners (free for this public repo).
- **Distribution:** TestFlight, the Apple Developer Program.

**Shared:**
- design tokens in one DTCG JSON, generating the hub's CSS variables and the app's colour catalog;
- card notation per ADR 0010;
- realtime per ADR 0009;
- sessions per ADR 0011.

## Consequences
- **The gate grows three blocks:** `--backend` moves to `backend/`, plus `--hub` and `--ios`. The iOS block reports itself "not proven" where Xcode is missing.
- **Owner actions:** install Xcode 27; join the Apple Developer Program before the first install on a friend's phone; JDK 21 is already installed.
- **The Core ML export runs in its own pinned Python environment,** because coremltools 9 needs numpy ≤ 2.3.5 and `cv/` locks 2.5.3.
- **Java 25 (LTS to 2031) is a cheap later upgrade;** 21 was chosen because it is installed and in Spring Boot 4.1's range (17–26).
