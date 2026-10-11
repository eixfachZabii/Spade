# Phase 10 · First night

> **Status:** 🔲 ready, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Issues:** [#8](https://github.com/eixfachZabii/Spade/issues/8) (closes), [#9](https://github.com/eixfachZabii/Spade/issues/9) (closes)
> **Depends on:** phases 06–09 · **Rests on:** V1 D2, D14, D15 · ADR [0007](../adr/0007-spade-runs-on-the-owners-linux-pc-behind-a-cloudflare-tunnel.md)
> **Owner first:** register the domain and move its DNS to Cloudflare; install Docker and `cloudflared` on the PC.

## Outcome
Spade runs on the owner's Linux PC behind a Cloudflare Tunnel on our own domain, guests can install the app from a public TestFlight link, and **the group plays a real poker night on it.** That night is V1's acceptance test.

## Scope
### In
- **The image:** a `Dockerfile` (multi-stage: the backend with the built hub inside, Java 21 runtime).
- **The compose stack:** `docker-compose.yml` with backend, PostgreSQL and `cloudflared`.
  - health checks; restart policies; starts with the PC;
  - `.env` on the PC only (template in the repo).
  - The legacy Docker files are not repaired (#9).
- **The tunnel and DNS:** `spade.<domain>` (name chosen at the start) → the backend; heartbeats verified through the tunnel.
- **Backups:** a nightly `pg_dump` with rotation, and **one restore drill** into a fresh stack.
- **Docker in the gate:** a compose smoke test (build the image, start the stack, hit `/actuator/health` and the testworld scripted hand). The gate's "Docker image was not built" line becomes a check.
- **Guests:** a TestFlight external build with a public link. Beta App Review gets a demo account on a seeded table, and the PC stays on during review.
- **The runbook:** USAGE.md "Start a poker night", aiming for **under five minutes** from "cards are out" to the first hand (PRODUCT.md), measured.
- **The dress rehearsal:** the testworld scripted night on the real stack, through the tunnel, with the real hub screen and at least three iPhones.
- **The night:** a real poker night on Spade. Then a **night report** in this doc:
  - setup time;
  - hands played;
  - misreads and how long each correction took;
  - chip counts needed;
  - crashes and reconnects;
  - what the group said.

### Out (and where it goes)
| What | Where |
|---|---|
| the ledger pages (the night's buy-ins and cash-outs are recorded since 06) | 11 |
| the extras | 12 |
| the table camera | 13 |

## Decisions
| # | Decision |
|---|---|
| D1 | **The night report decides follow-ups:** each problem becomes an issue (`capture-idea`) before the phase ships. |
| D2 | **If the night stalls,** the group finishes on paper and the report says where; the phase ships only after a night that didn't stall. |

## Done when
- A full real poker night ran on Spade without falling back to paper.
- The setup was measured under five minutes.
- The restore drill worked.
- The compose smoke test is in the gate and CI.
- The night report is written.

## Risks
- **Home internet and the PC are single points of failure.** Phones on cellular survive a Wi-Fi drop; the PC doesn't survive a power cut.
- **Apple's review** may ask for things a private friends' app can't easily give.

## Not proven by this phase (expected)
- more than one night;
- other venues (the PC must be where the night is, or reachable).

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
