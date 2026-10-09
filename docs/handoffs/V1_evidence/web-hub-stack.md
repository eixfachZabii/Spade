# The web hub stack, and three design directions

> **Evidence, 2026-10-09.** Item 5 for the V1 grill ([INDEX](../INDEX.md)). A read-only research subagent (Opus) wrote §1–9. The coordinator wrote the **design directions** (§10) and the [draft DESIGN.md](design/DESIGN.md), and re-checked the library claims the iOS research disagreed with.
> The only measurement here is one bundle size; nothing else was built or run. The mockups are throwaway HTML.

## The short version
- **The decision that matters is the realtime model, not a library:**
  - players' phones send commands (fold, call, raise, scan, correction) as REST calls;
  - every client receives **its own view of the table as a full snapshot**, on connect and on every change;
  - the hub's view is computed from public state only, so before showdown **the hub never holds a hole card**, rather than merely not rendering one.
- **Stack:** Vite 8 + React 19 + TypeScript 6.0 as a static SPA served by Spring Boot (one origin, one process), TanStack Query + Router, Tailwind 4, Motion 14. It's the owner's WealthWatcher stack, a version newer.
- **The hub logs in by pairing:** the TV shows a code and a QR, the owner approves in the iPhone app, and the hub gets a revocable, read-only `HUB` token. Never the owner's own session: the owner is also a player.
- **Design tokens:** one DTCG JSON file and a ~50-line script that writes the CSS variables and the Xcode colour catalog.
- **Three design directions** are ready for the owner to choose from (§10). The grill picks; this doc does not.

## Recommendation
| Concern | Choice | Why | Alternatives (trade-off) |
|---|---|---|---|
| Framework | **Vite 8 + React 19 + TypeScript 6.0**, a static SPA | No server rendering needed; the owner's stack; deepest ecosystem (Motion); AI assistants write it best; the build is static files Spring Boot can serve | **SvelteKit 3** in SPA mode: smaller runtime and built-in transitions, but a ~1-week-old major the owner doesn't know. **React Router 8** framework mode, `ssr:false`: a build-time render step for no gain |
| TypeScript | **pin 6.0.x, not 7** | `typescript-eslint` 8.71.1 declares `typescript >=4.8.4 <6.1.0` | TS 7 for type-checking only, once lint catches up |
| Router | **TanStack Router 1.170**, hash history | The owner uses it; ~5 routes; hash history needs no server fallback | React Router 8 declarative |
| Realtime transport | **SSE for hub and iPhone** | Only server→client needs pushing. The stream is a plain GET behind the same Spring Security filters as REST, which closes #2's STOMP CONNECT/SUBSCRIBE holes by construction. `EventSource` is built in and reconnects itself; Swift has a maintained client (LDSwiftEventSource 3.4.0, checked) | **STOMP:** best web client (`@stomp/stompjs` 7.3), but the only Swift library was last released in May 2024. **Plain WebSocket + JSON:** native on both sides, with hand-written auth and heartbeats (the [iOS research](ios-stack.md#4-realtime-client)'s pick) |
| Client state | **The TanStack Query cache** as the only store of server data; snapshots go in with `setQueryData`, `staleTime: Infinity` | REST first load and the stream write the same key; no extra store | Zustand 5 if UI-only state grows |
| REST + typed client | **springdoc** → committed `openapi.json` → **openapi-typescript + openapi-fetch** for the web and **swift-openapi-generator** for iOS | One contract for both clients; expose the snapshot as a GET too, so its type is generated | **orval 8** (generates Query hooks and MSW mocks); **hey-api** (pre-1.0, many open issues) |
| Charts (ledger) | **Hand-written SVG**, optionally `d3-shape` | ~20 nights × ~10 players; a TV has no hover, so labels sit on the lines | **Recharts 3** (quickest; ~148 kB gzipped whole); **visx 4** |
| Motion | **Motion 14** for one-off moments; **CSS keyframes** for anything that loops for hours (the turn pulse); the flip animates a full `transform` | Only `transform`/`opacity` are reliably GPU-composited; Motion's own docs say its separate `x`/`rotateY` props are not | CSS + View Transitions (all major browsers since 2025-10) |
| Styling | **Tailwind 4.3**, tokens in CSS (`@theme inline` over CSS variables); dark by default | Tokens stay runtime variables; owner fluency | CSS Modules + variables |
| Primitives | **none by default**; Radix pieces only where focus trapping matters | a display with few controls | React Aria if a TV remote's arrow keys drive it |
| Hub login | **device pairing** → opaque, revocable `HUB` token, read-only, scoped to the approving owner's tables | least privilege; nothing to type on a TV | a long-lived token in an env file or URL (leaks into logs); a shared "hub" account |
| Kiosk | Chromium `--kiosk` on a laptop or mini-PC; Wake Lock re-acquired; watchdog + `appVersion` reload; slight pixel shifting against OLED burn-in | Fullscreen needs a gesture; Wake Lock needs HTTPS; EventSource can't see a half-open connection | the TV's own browser (old engines; Tailwind 4 needs Chrome 111 / Safari 16.4 / Firefox 128) |
| Tokens shared with iOS | **one DTCG JSON + a ~50-line script** → `tokens.css`, an Xcode colour catalog (dark/light variants), `SpadeTokens.swift`; the gate checks they're current | ~30 tokens don't justify a toolchain | **Terrazzo** (its Swift plugin is "still experimental"); **Style Dictionary 5.6** |
| Tests and gate | Vitest 5 + Testing Library 16 + MSW 3; Playwright 1.64 against a **scripted fake table server**; screenshot tests with Linux baselines; a `--hub` gate block | The privacy guarantee is proven in the **backend** build, where it can be | testing against the real Spring backend in the hub block |

## Sources
All accessed 2026-10-09. Versions from `registry.npmjs.org/<pkg>` (`latest`), Maven Central or GitHub releases.

| Package | Version | Released | Notes |
|---|---|---|---|
| vite | 8.3.4 | 2026-10-08 | Vite 8 stable 2026-03-12, Rolldown only; Node 20.19+/22.12+ ([announcement](https://vite.dev/blog/announcing-vite8)) |
| react | 19.3.0 | 2026-09-09 | |
| @vitejs/plugin-react | 6.1.2 | 2026-10-05 | needs vite ^8 |
| typescript | 7.0.2 | 2026-07-08 | 6.0.3 released 2026-04-16 |
| typescript-eslint | 8.71.1 | | peer `typescript <6.1.0` |
| @sveltejs/kit | 3.0.1 | 2026-10-06 | 3.0 about 2026-10-01 (secondary sources) |
| next | 16.4.0 | 2026-10-06 | |
| react-router | 8.4.0 | 2026-09-15 | |
| @tanstack/react-router | 1.170.41 | 2026-09-30 | |
| @tanstack/react-query | 5.104.1 | 2026-10-02 | |
| zustand | 5.0.15 | 2026-08-13 | |
| @stomp/stompjs | 7.3.0 | 2026-01-31 | measured: 22.7 kB min, 6.5 kB gzipped |
| partysocket | 1.3.0 | 2026-06-23 | reconnecting WebSocket |
| openapi-typescript / openapi-fetch | 7.13.0 / 0.17.0 | 2026-02-11 | |
| orval | 8.41.0 | 2026-10-08 | |
| @hey-api/openapi-ts | 0.99.0 | 2026-06-22 | 652 open issues |
| recharts | 3.10.1 | 2026-07-25 | |
| @visx/xychart | 4.0.0 | 2026-06-11 | |
| d3-shape / d3-scale | 3.2.0 / 4.0.2 | 2022 / 2021 | stable, finished |
| motion | 14.0.0 | 2026-10-02 | 13→14 breaking changes are small |
| tailwindcss | 4.3.3 | 2026-07-16 | |
| radix-ui / react-aria-components | 1.7.0 / 1.22.0 | 2026-10 | |
| style-dictionary | 5.6.0 | 2026-10-03 | |
| @terrazzo/plugin-swift | 0.3.3 | 2026-07-26 | "still experimental" |
| vitest | 5.0.3 | 2026-09-30 | Node ^22.12 |
| @playwright/test | 1.64.0 | 2026-10-07 | |
| msw | 3.0.2 | 2026-10-03 | `msw/sse`, `msw/ws` |
| springdoc-openapi-starter-webmvc-ui | 3.1.1 (Boot 4) / 2.9.1 (Boot 3) | | |
| Spring Boot | 4.1.1 current | | backend today 3.4.4; see the support dates in the [evidence README](README.md) |
| apple/swift-openapi-generator | 1.14.0 | 2026-10-06 | |
| launchdarkly/swift-eventsource | 3.4.0 | 2026-09-29 | re-checked by the coordinator |

| Documentation | URL | What it established |
|---|---|---|
| STOMP.js | https://stomp-js.github.io/api-docs/latest/classes/Client | exponential reconnect, heartbeats in a Web Worker |
| Spring: STOMP token auth | https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication-token-based.html | interceptor at `HIGHEST_PRECEDENCE + 99` |
| Spring: async + SSE | https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html | `SseEmitter`; no disconnect notice, so send periodically |
| Spring Security 7: WebSocket | https://docs.spring.io/spring-security/reference/7.0/servlet/integrations/websocket.html | `AuthorizationManager<Message<?>>`; CSRF on CONNECT by default |
| Spring Boot: static content | https://docs.spring.io/spring-boot/reference/web/servlet.html | serves `classpath:/static` |
| springdoc | https://springdoc.org/ | OpenAPI 3.1 by default; build-time export plugins |
| MDN: EventSource | https://developer.mozilla.org/en-US/docs/Web/API/EventSource | only `withCredentials`, no headers; 6 connections per domain on HTTP/1.1 |
| MDN: Screen Wake Lock | https://developer.mozilla.org/en-US/docs/Web/API/Screen_Wake_Lock_API | HTTPS only; released when hidden; all major browsers since 2025-03-31 |
| TanStack Query defaults | https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults | `staleTime: Infinity` |
| Motion: performance | https://motion.dev/docs/performance | separate transform props are "not currently hardware accelerated" |
| Tailwind compatibility | https://tailwindcss.com/docs/compatibility | Chrome 111, Safari 16.4, Firefox 128 |
| Next.js static export | https://nextjs.org/docs/app/guides/static-exports | features lost in `output: 'export'` |
| Playwright WebSocketRoute / snapshots | https://playwright.dev/docs/api/class-websocketroute ; https://playwright.dev/docs/test-snapshots | baselines differ per OS |
| Design Tokens spec 2025.10 | https://www.designtokens.org/tr/2025.10/ | first stable version, 2025-10-28 |
| RFC 8628 (device grant) | https://datatracker.ietf.org/doc/html/rfc8628 | the pattern the pairing follows (not fetched) |

**Local paths read:**
- [PRODUCT.md](../../../PRODUCT.md), [CONTEXT.md](../../../CONTEXT.md), [frontends](../../status-quo/frontends.md), [salvage](../../status-quo/salvage.md), [webapp style](../../references/webapp-style.md);
- issues #2, #6, #8, #12;
- `spadeboot/pom.xml` (Boot 3.4.4, `spring-security-messaging` present, no springdoc);
- `WebSocketConfig.java` (`/ws` registered twice; origins only `:3000`; no heartbeat scheduler);
- `WebSocketSecurityConfig.java` (never rejects a connection);
- `client/src/assets/images/poker/card_deck/` (75 PNGs at 500×726, 8.4 MB);
- WealthWatcher's `client/package.json`: React ^19.2, Query ^5.100, Router ^1.170, Zustand ^5.0.14, Motion ^12.40, **Tailwind ^3.4**, Vitest ^3.2.7, Playwright ^1.60, `rolldown-vite` 7.2.5.

## 1. Framework
| Criterion | Vite + React SPA | SvelteKit 3 (SPA) | Next.js 16 (`output:'export'`) | React Router 8 (`ssr:false`) |
|---|---|---|---|---|
| Server rendering | none; fits the hub | optional | its main selling point, unused here | off, but the root still renders at build time |
| Runtime on a small box | larger React runtime; irrelevant for a LAN page loaded once a night | smallest | React + Next's router | React |
| Animation | Motion, CSS | built-in `transition:`, `animate:flip` | Motion | Motion |
| Owner familiarity | **high** | none | some | some |
| AI-assistant fluency | highest | lower (Svelte 5 runes vs older training data; judgement) | high, but server-assuming | high |
| Maturity now | Vite 8 stable since 2026-03 | 3.0 about a week old | stable | stable |

**Verdict: Vite + React + TS.**
- On a 4K TV driven by a weak box, the cost is compositing pixels, not React re-rendering a small table a few times a minute (judgement).
- **Serve the hub from Spring Boot** (`hub/dist` → `static/`): one origin, so no CORS (the legacy `:3000`-only origin list was a bug), an HttpOnly cookie works for the stream, and one process runs on the night.
- Hash history or a forwarding controller handles client routes. Vite's dev server proxies `/api`.
- Pins: TypeScript ~6.0, Vite ^8, plugin-react ^6, Node ≥22.12. WealthWatcher's `rolldown-vite` override is no longer needed.

## 2. Realtime
**The model (independent of transport)**
- **Commands over REST**: `POST /tables/{id}/actions`, `/scans`, `/corrections`. Validated, idempotent via a client-generated id, in OpenAPI.
- **State pushed as one projection per viewer:**
  - `PublicTableView` for the hub, with **no hole-card field at all** before showdown, and `revealed` hands at showdown;
  - `PlayerTableView` = the public view + `you` (seat, to-call, legal actions, your own cards).
- **Resync = snapshot on connect, snapshot on every change.** Every snapshot carries `tableId`, a monotonic `version`, `handId` and `appVersion`; clients drop older versions.
- **Animations come from comparing snapshots:**
  - the board flip fires when the board grows within the same `handId`;
  - the winner moment fires when `lastHandResult.handId` changes;
  - after a reconnect the hub snaps to the current state instead of replaying flips.
- `lastHandResult` stays in the snapshot until the next hand, so a reconnecting hub still shows the announcement.
- Win probability (#12) fits the same way: an `equity` field the server fills only once cards are face up.

| | **SSE** (recommended) | **STOMP over WebSocket** | **Plain WebSocket + JSON** |
|---|---|---|---|
| Web client | `EventSource`, 0 kB, reconnects itself | `@stomp/stompjs` 7.3, 6.5 kB gz; backoff, heartbeats | `WebSocket` + `partysocket` 1.3 or ~60 lines |
| Browser auth | no headers: HttpOnly cookie (same origin) or a 30-s single-use ticket | token in the CONNECT frame | cookie, ticket or first message |
| Dead connection | server sends real `event: ping`; client watchdog (30 s) | built in once the server sets heartbeats and a `TaskScheduler` (missing today) | hand-written |
| Spring | `SseEmitter` per connection, a registry by table, per-viewer projection, ~15 s ping; **the same filter chain as REST**; test Spring Security's async handling | broker; CONNECT interceptor that **throws** without a token; SUBSCRIBE rules; CSRF-on-CONNECT off; `@SubscribeMapping` for the first snapshot | `TextWebSocketHandler`, handshake interceptor, registry, `ConcurrentWebSocketSessionDecorator` |
| Privacy | per connection: private data has nowhere to go but its owner | correct only if every SUBSCRIBE rule is right (#2's hole) | per connection |
| iOS | LDSwiftEventSource 3.4.0 or a short `URLSession.bytes` parser | stale library or a hand-written codec | native |

**Getting snapshots into React:**
1. `GET /api/tables/{id}/view` returns the same DTO as the stream.
2. Each pushed snapshot goes in via `setQueryData(['table', id, 'view'], next)` unless its `version` is older.
3. `staleTime: Infinity`.
4. One `invalidateQueries` after a reconnect, as a safety net.

One `useTableStream(tableId)` hook owns the connection, the watchdog and the backoff; components only call `useQuery`.

## 3. Data fetching and the typed client
- **TanStack Query 5.104.**
- **The contract:**
  1. springdoc in the rebuilt backend (2.9.1 on Boot 3, 3.1.1 on Boot 4);
  2. `mvn verify` exports `api/openapi.json`;
  3. the file is committed, and the gate fails on drift.
- **Web:** openapi-typescript (types only) + openapi-fetch (2.8 kB gz), wrapped in hand-written `queryOptions()` factories. Alternative: orval 8 if the API grows past ~30 endpoints.
- **Swift:** swift-openapi-generator 1.14 accepts OpenAPI 3.0/3.1 (untested against springdoc's output; see Not proven).
- **Card art:** the asset names (`jack_of_clubs.png`, plus an `…A.png` variant set) are a fourth notation. Map them in one function from #5's canonical notation, with a test over all 52 cards. That test catches the legacy "Jacks render as Aces" class of bug.

## 4. Charts for the ledger
Cumulative P&L per player over ~20 nights, plus a bar per player: at most ~200 points. Nobody hovers on a TV, so labels go at the end of each line, large and coloured to match.

| Option | Size (gz, whole package) | Fit |
|---|---|---|
| **Hand-written SVG** (+ `d3-shape`, 5.5 kB) | 0–6 kB | full control of type size and direct labels; two chart types is little code |
| Recharts 3.10.1 | 148 kB (less tree-shaken) | fastest to build; TV-hostile defaults (small type, tooltips) |
| visx 4.0 | 48.8 kB | building blocks; as much code as hand-written |
| Chart.js 4.5.1 / uPlot 1.6.32 | 66.8 / 21.3 kB | canvas; config-styled / time-series focused |

## 5. Motion
| Need | Technique |
|---|---|
| Board flip | two faces with `backface-visibility: hidden` in a `perspective` container; animate a **full `transform` string** or CSS. **Decode the card image before flipping** |
| Whose turn (pulses for hours) | CSS keyframes on a pseudo-element ring's `transform`/`opacity`. **Never animate `box-shadow`**: it repaints every frame, costly at 4K (judgement) |
| Winner moment, pot splits, chips moving | Motion `AnimatePresence`, `layout`, springs |
| Seat changes | Motion `layout` or auto-animate |
| Page switches | View Transitions API or `AnimatePresence` |
| Reduced motion | `<MotionConfig reducedMotion="user">`, the CSS media query, and **a hub setting**, because TV boxes rarely expose the OS preference |

**On a weak box driving a 4K TV:** output **1080p** and let the TV upscale (a quarter of the pixels to composite; the biggest single lever). Keep `backdrop-filter` off large persistent layers.

## 6. Styling, tokens, components
- **Tailwind 4.3, CSS-first:**
  - generated `tokens.css` holds the variables (`:root`, `.light`);
  - `@theme inline` maps them to utilities that read the variables at runtime;
  - WealthWatcher is still on Tailwind 3.4, so this setup is a small new thing for the owner.
- **Fonts and images self-hosted:** `@fontsource-variable/inter` 5.3, with the card images bundled and preloaded. A LAN on poker night may have no internet, so no font CDN.
- **Primitives:** plain elements with visible focus for pairing, settings, ledger filters and the chip helper. Add Radix Dialog/Tabs only where focus trapping matters. Icons: `lucide-react`.

## 7. How the shared screen logs in
| Option | Least privilege | Revocable | Typing on the TV |
|---|---|---|---|
| **Pairing code + QR, approved in the iPhone app** (RFC 8628 style) | `HUB` role, public view only | yes, from the app | none |
| Long-lived token in a URL or env file | depends | only by editing the server | paste once; leaks into history and logs |
| The owner's own session | **no**: the owner is a player, so their private view would reach the TV | logout | password |
| Shared "hub" account | role-based | change the password | yes |

**Flow**
1. The hub calls `POST /api/hub/pairings` (unauthenticated, rate-limited) and gets `{pairingId, userCode, pollSecret, expiresAt}`: an 8-character code without look-alike characters, valid 10 minutes. The TV shows the code and a QR.
2. The owner scans or types it in the app, which calls `POST /api/hub/pairings/{userCode}/approve` (authenticated).
3. The hub polls with `pollSecret` and receives a single-use response holding an **opaque 256-bit token**.
4. The server stores only its **SHA-256** (CLAUDE.md: compare secrets by hash), in a `hub_device` table with name, owner, scope, last seen and revoked-at.
5. The hub keeps the token as an HttpOnly, Secure, SameSite=Strict cookie.
6. The app can list and revoke hubs; revocation is immediate (opaque tokens are checked on every request).

**What the hub may do**
- Read the public table view, the ledger, the cheatsheet, names and avatars.
- **No command endpoints, no private projection; it doesn't start hands by default.**
- Its principal is `hub:<id>`, so it never collides with a user name.
- A backend test proves the `HUB` stream carries no hole card before showdown.

**Network:** without HTTPS both the token cookie and Wake Lock fail, so #8's certificate decision applies to the hub too.

**Kiosk**
- Fullscreen needs a gesture; a "click to start" screen is the fallback.
- Re-acquire Wake Lock on `visibilitychange` and disable OS sleep as well.
- Reconnect: the watchdog with backoff, plus an immediate reconnect on `online`.
- `appVersion` mismatch → reload; 5 minutes without a snapshot → reload.
- OLED: the TV's pixel shift on; nudge static elements a few px every few minutes; dim when idle.

## 8. Design tokens shared with iOS
- Keep one `tokens/spade.tokens.json` (DTCG 2025.10: `$value`/`$type`, dark/light per colour) with a dependency-free `tokens/build.mjs` of ~50 lines. It emits:
  - `hub/src/styles/tokens.css` (`:root`, `.light`, `@theme inline`);
  - `ios/…/SpadeTokens.xcassets/<name>.colorset/Contents.json` with a dark appearance;
  - `SpadeTokens.swift` for radii, spacing and durations.
- `--check` in the gate keeps them current. This assumes one repo (ADR 0001).
- Alternatives: Terrazzo 2.7 (Swift plugin experimental); Style Dictionary 5.6 (mature; dark/light colour catalogs need custom work).

## 9. Testing and the gate
- **Unit:** Vitest 5 + Testing Library 16 + jsdom, covering the snapshot reducer (version ordering, which diff triggers which animation), the card-asset mapping for all 52 cards, and seat-oval geometry (the salvaged `positionUtils`). MSW 3 mocks REST and `msw/sse`.
- **e2e:** Playwright 1.64 against a **scripted fake table server** (a small Node script via `webServer`) replaying fixture hands:
  - heads-up;
  - ten players;
  - an all-in with side pots;
  - a split pot;
  - a reconnect mid-hand.
- **Screenshots:** `toHaveScreenshot` of the table view at 1920×1080 with DPR 2. Baselines are made **on Linux** (Playwright's Docker image), in a separate config.
- **Privacy is proven in `mvn verify`:** connect as `HUB` and as two players, play a scripted hand, and assert that no hub-bound message and no `/view` response holds a hole card before showdown, and that each player gets only their own.
- **`--hub` gate block:** `npm ci`, `tsc --noEmit`, eslint, `vitest run`, `vite build`, `playwright test`, the token drift check, the OpenAPI drift check.
- **New "not proven" lines:**
  - no real TV, box, 4K performance or OLED behaviour;
  - no real Wi-Fi drop;
  - Linux Chromium baselines only;
  - Wake Lock and kiosk mode on the target device;
  - the fake server is not the backend.

## 10. Design direction: three table views for the owner to choose from
**How they were made:**
- `/impeccable` (v4.0.4), starting from [the webapp's style](../../references/webapp-style.md), which the owner pinned.
- All three keep the webapp's world:
  - dark-first Tailwind grays;
  - the purple→blue gradient, reserved for **one** thing (whose turn it is, and the winner);
  - green stacks, amber all-in;
  - 12 px radii scaled up for TV panels;
  - the flip and the pulse.
- They differ in **structure and type**.
- Each is a static HTML mockup with two states (`#turn`, `#showdown`; press **S** to toggle), built on a 1920×1080 canvas that scales to any 16:9 screen.
- The data is synthetic: six invented players, hand 23 on the turn with a side pot, then a full-house showdown.

| | **A · The Oval** | **B · Spotlight** | **C · Departures** |
|---|---|---|---|
| Mockup | [a-oval.html](design/a-oval.html) | [b-spotlight.html](design/b-spotlight.html) | [c-departures.html](design/c-departures.html) |
| Idea | The screen is the table: six seats on an oval rim where they really sit; pot, board and the last action in the middle | **Whoever must act owns the screen.** A gradient panel with the name at 168 px and the amount to call; board and pot to the right; a rail of seats in acting order | **The table as a split-flap board.** Board tiles and pot in flaps; one ruled row per seat (cards, stack, bet, last action, status); lamps instead of badges; changes clack through |
| Answers from across the room | "where is everyone, what's on the board" | "who is it on, how much" | "everything, row by row" |
| Type | Inter (the webapp's face, unchanged) | Archivo, widened for names and figures | Barlow Condensed, caps, in fixed cells |
| Where it came from | the category standard and the owner's own 2025 concept (the legacy hub's seat oval) | the structure dealt by impeccable's seed roll (`c64b18ec`, 4 of 7) | a challenger the same roll dealt (rail concourse split-flap board), fused with the webapp's world |
| Signature moment | the board flip, and a gradient "Elif wins €15.00" banner over the felt | the panel turns from the actor into the winner, hole cards and all | the flap cascade when a value changes; the winning tiles ringed |
| Honest risk | the most familiar, so the least memorable; seat positions must be configured per table; dense with 9–10 seats | long names (e.g. "Konstantin") need fit-to-width; the other seats are secondary; a strong look that could tire over a long night | the most opinionated; caps and cells read slower for prose (announcements); the flap motion must stay rare or it becomes noise |
| Carries to the iPhone app | seat colours, card design, stacks | the gradient panel as the player's own "your turn" screen | lamps and the status vocabulary |

**Screenshots:** `.playwright/screenshots/v1-hub-{a-oval,b-spotlight,c-departures}-{turn,showdown}.png`. They're not committed; re-create them by serving `design/` and opening each file at 1920×1080.

**The finish pass, as run:**
- Two batched screenshot rounds; then impeccable's detector, run once.
- Fixed in the second round:
  - a rotated corner index made a 9 read as a 6, so the cards now carry one index;
  - seat labels overlapped names;
  - wrapped headings in B;
  - in C, a lost row highlight, a table overflowing its footer, and columns that moved between states;
  - in C, a coloured glow replaced by a ring and a neutral shadow.
- **Still open:**
  - C's last-action column clips "ALL-IN €2.40";
  - the detector flags Inter as an overused face; it is kept on purpose in A, as the owner's pinned style, and is a grill question;
  - A's turn pulse animates `box-shadow` (fine in a mockup, not in the build; see §5).
- Impeccable's separate finish-reviewer agent and documenter were **not** run. These are throwaway inputs to a choice; the chosen direction gets that review in its V1 phase.

**The [draft DESIGN.md](design/DESIGN.md)** holds what all three share (tokens, card design, motion rules, the TV type scale) and a section per direction. It moves to the repo root only after the grill picks.

## Open questions for the V1 grill (recommended default first)
1. **Push transport:** **SSE**, settled together with the iOS side ([ios-stack §4](ios-stack.md#4-realtime-client)).
2. **Who serves the hub:** **Spring Boot's `static/`** (same origin, one process).
3. **May the hub start the next hand?** **No**; a phone does.
4. **What a hub token sees:** **the approving owner's tables**, following the active one automatically.
5. **Light theme on the hub:** **dark only**; the tokens keep light values for iOS.
6. **Ledger charts:** **hand-written SVG** with labels on the lines.
7. **Motion:** **full flip and winner moment**, with a reduced-motion switch in hub settings.
8. **Display hardware:** **a laptop or mini-PC running Chromium in kiosk mode, 1080p output.**
9. **Idle behaviour:** **rotate to the ledger after 5 minutes idle; dim after 15.**
10. **Folder name:** **`hub/`** (the glossary's word).
11. **Card art:** the plain faces **are not needed**, because the mockups draw cards in CSS with one big index for distance; keep the PNG deck only for the cheatsheet. Check the PNGs' origin before publishing.
12. **Design direction:** A, B or C (no default; the owner picks).

## Not proven
- **Nothing was built** beyond static mockups; no performance measured except the STOMP.js bundle size.
- **Vendor claims:**
  - Vite's build speed;
  - Motion's 4.6 kB `LazyMotion` and its acceleration statements;
  - Tailwind's browser floor;
  - swift-openapi-generator's 3.1 support;
  - Terrazzo's Swift output.
- **Bundle sizes** are bundlephobia's whole-package numbers.
- **Secondary sources:** SvelteKit 3's release date and breaking changes.
- **Inferred, not tested:**
  - that TS 7 breaks the lint setup;
  - that springdoc's output feeds swift-openapi-generator cleanly;
  - that Spring Security handles `SseEmitter`'s async response here;
  - that snapshots stay a few kB.
- **Judgement, not measured:** 1080p beating 4K on a weak box; the cost of `box-shadow`/`backdrop-filter`; React's re-render cost; assistants' Svelte fluency.
- **The mockups were viewed in Playwright's Chromium at 1920×1080 only:** not on a TV, not from 3 m, not at 4K, not by the group.
