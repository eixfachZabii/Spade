# V1 evidence: what the grill reads first

> **Dated 2026-10-09.** The evidence [INDEX](../INDEX.md) asks for before Version 1's grill. Each doc has sources, a recommendation with alternatives, and a "Not proven" section. They are snapshots; they are not maintained after the grill.

| # | Evidence | Doc | State |
|---|---|---|---|
| 1 | Card-detection spike (#3) | [cv-proxy-spike.md](cv-proxy-spike.md) | **proxy only.** The real spike needs the deck, the mat and an overhead camera; #3 stays open |
| 2 | Salvage map | [salvage.md](../../status-quo/salvage.md) | done in V0 |
| 3 | WealthWatcher's architecture | [wealthwatcher-architecture.md](wealthwatcher-architecture.md) | done |
| 4 | iOS stack (incl. a measured Core ML export) | [ios-stack.md](ios-stack.md) | done |
| 5 | Web hub stack and design directions | [web-hub-stack.md](web-hub-stack.md), [design/](design/DESIGN.md) | done; the owner picks a direction in the grill |

## What cuts across the docs
- **One realtime model, with the transport still open.**
  - Both stack researches land on the same model: REST for every command, and a full **per-viewer snapshot** pushed on connect and on every change.
  - The hub's projection has no hole-card field before showdown, so privacy is structural.
  - They differ only on the pipe: SSE (hub research) vs plain WebSocket (iOS research). The iOS argument against SSE ("no Swift client") was checked and is wrong: LDSwiftEventSource 3.4.0, 2026-09-29.
- **Contract discipline.**
  - WealthWatcher's hand-mirrored TypeScript types drifted.
  - All three docs point to one committed `openapi.json` and contract-snapshot bodies that the hub and the app decode in their tests.
- **The engine is a fold over events**, so a manual board entry, a camera read and a correction are just different events: "board reading must not block the night" falls out of the design ([wealthwatcher-architecture.md](wealthwatcher-architecture.md#a-a-slimmed-spring-boot-backend)).
- **The hub logs in by pairing** with a read-only `HUB` token, never the owner's session.

## The backend's framework version is a decision too
Checked 2026-10-09 against [endoflife.date](https://endoflife.date/spring-boot) and the [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html):

| Spring Boot | Released | Open-source support ends |
|---|---|---|
| 3.4 (today's `spadeboot/`, 3.4.4) | 2024-11 | **2025-12-31: already over** |
| 3.5 | 2025-05 | **2026-06-30: already over** |
| 4.0 | 2025-11 | 2026-12-31 |
| 4.1 (current: 4.1.1) | 2026-06 | 2027-07-31 |

- Spring Boot 4.1.1 needs Java 17–26 and Spring Framework 7.0.9+.
- Java 21 is installed on the owner's Mac; Java 25 (LTS until 2031) is not.
- A rebuild on 3.x starts on an unsupported line. **Grill question: Spring Boot 4.1 on Java 21 or 25.**

## Owner actions the evidence surfaced
- Install **Xcode 27** (not installed; nothing iOS can be built until it is).
- Join the **Apple Developer Program** (99 USD/year) before the first install on a friend's phone (TestFlight internal).
- Decide hosting (#8): it decides certificates for the app *and* the hub (Wake Lock and secure cookies need HTTPS).
- When the cards and mat are back: the real spike (#3).
