# 9. Commands go over REST; every client receives its own snapshot over SSE

Date: 2026-10-10
Status: Accepted
Relates to: [#2](https://github.com/eixfachZabii/Spade/issues/2) · [web-hub-stack.md §2](../handoffs/V1_evidence/web-hub-stack.md#2-realtime) · [ios-stack.md §4](../handoffs/V1_evidence/ios-stack.md#4-realtime-client) · [ADR 0007](0007-spade-runs-on-the-owners-linux-pc-behind-a-cloudflare-tunnel.md) · V1 grill

## Context
The 2025 backend pushed STOMP events, but most were never sent, CONNECT needed no token, SUBSCRIBE was never authorised, and every client received every hole card (#2). No maintained Swift STOMP client exists. Both stack researches arrived at the same model and differed only on the pipe: SSE or plain WebSocket.

## Decision
- **Every command is a REST call:** act, start a hand, read or correct cards, count a stack. Each is validated, idempotent (a client-generated id) and in the OpenAPI contract.
- **Every client receives full snapshots of the table, projected for that viewer:**
  - on connect, and after every committed change;
  - each with a monotonic `version`; clients drop anything older;
  - after a reconnect, the first snapshot is the resync. No event replay.
- **Projections:**
  - the hub's view is built from public state only and **has no hole-card field** until showdown;
  - a player's view adds that player's own seat: their cards, to-call and legal actions;
  - one projection function per viewer kind, and a backend test that no viewer ever receives another seat's hole cards before showdown.
- **The pipe is Server-Sent Events.** One authenticated GET per client, behind the same Spring Security filter chain as REST.
  - Heartbeats go out every 15–30 s (under Cloudflare's idle limit), plus client watchdogs.
  - Clients: `EventSource` in the hub, LDSwiftEventSource or a short `URLSession.bytes` parser in the app.
  - The snapshot DTO is also served by a GET, so both clients generate its type from OpenAPI.
- **STOMP and the `/ws` endpoint are dropped.**

## Considered options
- **Plain WebSocket + JSON.** Native on both platforms, but auth, heartbeats and subscriptions are hand-written, and the client-to-server half would go unused.
- **Keep STOMP.** The best web client, but no maintained Swift client; the broker's SUBSCRIBE rules are exactly the hole #2 found.

## Consequences
- Privacy becomes structural: data a viewer may not see is never put in their projection.
- Animations (the board flip, the winner moment) are derived on the client by comparing consecutive snapshots.
- Swapping the pipe later touches one stream endpoint and two client hooks; the model stays.
