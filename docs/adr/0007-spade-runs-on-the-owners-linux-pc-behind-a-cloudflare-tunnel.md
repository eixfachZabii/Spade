# 7. Spade runs on the owner's Linux PC, behind a Cloudflare Tunnel on our own domain

Date: 2026-10-10
Status: Accepted
Relates to: [#8](https://github.com/eixfachZabii/Spade/issues/8) (hosting) · [#9](https://github.com/eixfachZabii/Spade/issues/9) (Docker) · [ios-stack.md §6](../handoffs/V1_evidence/ios-stack.md#6-local-network-ats-local-network-privacy-and-8) · [web-hub-stack.md §7](../handoffs/V1_evidence/web-hub-stack.md#7-how-the-shared-screen-logs-in) · V1 grill

## Context
iPhones only talk to a server with a trusted certificate unless the app carries exceptions (App Transport Security). Talking to a LAN address also triggers iOS's Local Network prompt. Guests install the player app through TestFlight, and a public TestFlight link needs Apple's beta review to reach a working backend. The hub needs HTTPS too, for its secure cookie and the Screen Wake Lock.

`poker-spade.de`, which the legacy config pointed at, is unregistered (DENIC: "free", 2026-10-10).

The owner has a Linux PC (Ryzen 7950X3D, RTX 4080) at the place where the nights happen, switched on while they play.

## Decision
- **Host:** backend, PostgreSQL and the hub's static files run on that PC as one Docker Compose stack, started with the PC. The table-camera service (cv) can join the stack later and use the GPU.
- **Address:**
  - The stack is published through a **Cloudflare Tunnel** (`cloudflared`, free) on a domain the group registers and keeps its DNS on Cloudflare.
  - The phones and the hub use one public HTTPS name (e.g. `spade.<domain>`) on Wi-Fi or cellular.
  - Nothing is port-forwarded; TLS ends at Cloudflare's edge, and the tunnel encrypts the leg to the PC.
- **Database:** PostgreSQL, schema by Flyway. Backend tests use Testcontainers PostgreSQL, so the gate needs Docker running.

## Considered options
- **A small cloud server.** Always on, but a monthly bill and a second machine to look after, while a capable PC already sits at the table.
- **Tailscale Funnel** (free, no domain, `*.ts.net`). Simplest, but the owner wants the group's own name.
- **LAN only** (a self-signed certificate pinned via QR). No internet exposure, but Local Network prompts, ATS exceptions, and no public TestFlight link for guests.

## Consequences
- **Owner actions:** register the domain and move its DNS to Cloudflare; install Docker and `cloudflared` on the PC.
- **The realtime stream must send a heartbeat well under Cloudflare's idle limit** (~100 s on free plans): every 15–30 s.
- **Spade only works while the PC is on.** During Apple's beta review of a public TestFlight build, keep the PC on with a demo account seeded, or add guests as internal testers instead.
- **Docker is in V1 after all.** The broken legacy compose setup (#9) is replaced, not repaired. The gate's "Docker image was not built" line stays true until a compose smoke test exists.
- **The backend's secrets** (`.env`) live on the PC, never in the repo.
