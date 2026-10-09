> **Raw audit, 2026-10-09.** Read-only subagent report, kept as evidence for [the V0 spec](../V0_2026-10-09_FOUNDATION.md). Not maintained: Phase 04 turns it into `docs/status-quo/`. Paths and line numbers were true on the audit date.

I've finished the read-only audit; no files in the repo were changed. A few previews were written to the scratchpad only.

# Status-quo audit: `client/` and `webapp/` frontends

## TL;DR
- **`client/` ("SpadeHub")** is Creative Tim × Simmmple **Vision UI Dashboard React** (free MIT template; `manifest.json`, `genezio.yaml` name `vision-ui-dashboard-react`, `avatar-simmmple.png`) on React 17.0.2, MUI 5.17, react-router 6.30. It is the **big-screen / hub app**: a read-only table display, plus a cheatsheet, Spotify, static analytics, profile and about pages. **It cannot create, join or play a game.**
- **`webapp/` ("Spade")** is plain CRA on React 19.0, hand-written CSS, with **no router**. It is the **per-player device app**: lobby, create/join table, the betting buttons over STOMP, and a phone-camera hole-card scanner plus owner camera calibration over socket.io.
- **The CV half is dead.** socket.io connects to Spring on :8080, which has no socket.io server. The CV service is not in this repo or its history.
- **The backend never broadcasts a winner, a turn, or community-card events**, so no showdown or winner UI exists anywhere.
- **Hygiene problems in a public repo:** TLS private keys are tracked, `gurobi.lic` is in master history, and a JWT secret and DB password are in branch history. `node_modules/` and `build/` are **not** tracked.

## 1. Stack and tooling

| | `client/` | `webapp/` |
|---|---|---|
| Base | Vision UI Dashboard React (template v3.0.0), CRA 5 (`react-scripts 5.0.1`) | CRA 5 |
| UI | MUI 5 + emotion, Vui* wrapper components, react-apexcharts, AnyChart (loaded from CDN), react-icons | Hand-written CSS (`src/styles/*.css`, ~3.8k LOC), react-icons, react-webcam |
| Realtime | none (REST polling every 2s) | `@stomp/stompjs` 7.1 + `sockjs-client` (listed as a devDep), `socket.io-client` 4.8 |
| `run.sh` | `HTTPS=true SSL_CRT_FILE=../client/cert.pem … npm start` | the same, plus `NODE_TLS_REJECT_UNAUTHORIZED=0 HOST=0.0.0.0` (LAN access so phones can use the camera; `getUserMedia` needs HTTPS) |
| Env | `.env.development`: `REACT_APP_API_BASE_URL=https://localhost:8080/api`, `WS=wss://localhost:8080/ws`. `.env.production`: relative `/api`, `/ws` (identical in both apps) | same |
| Config | `src/config/environment.js` (duplicated in both apps; webapp's adds `getWebSocketUrl()`) | same |

Further details:
- **Proxy:** `"options": {"proxy": "https://localhost:5000/"}` is nested under `options`, so CRA ignores it. Port 5000 is Flask's default, which hints at the old CV server.
- **Phones and dev env:** from a phone, `https://localhost:8080` resolves to the phone itself. LAN play therefore needs the env file edited.
- **`genezio.yaml`:** leftover template deploy config.
- **Real production:** `hub.poker-spade.de` (Spotify redirect in the untracked `application-prod.yml`; backend on :5467 with SSL off, so behind a reverse proxy) and `poker-spade.de` (avatar URLs on the About page).
- **Dev certs:** both apps share a self-signed cert.
  - Working copy: `CN=spade-dev.local`, no SAN, PKCS12-exported key with `friendlyName: spadeboot` (the same key as the backend keystore).
  - The committed HEAD copy is `CN=localhost` and expired on 7 Jul 2026.
  - Either way, `https://localhost:8080` gives cert-name mismatches.
- **CORS:** REST allows :3000 and :3001, but **STOMP `setAllowedOrigins` allows only :3000** (`spadeboot/.../config/WebSocketConfig.java`). Whichever app runs second (on :3001) cannot open a WebSocket.

## 2. Why two frontends (inferred)
- **`webapp`** is the **player's phone**: you sit at a table, scan your physical hole cards with the front camera (`facingMode: "user"`), see them privately (tap to reveal) and tap Fold/Check/Call/Raise.
- **`client`** is the **hub / TV screen**: it shows the table the logged-in account sits at, with every player's hole cards face up ("Always show face up as requested", `PokerGameService.js`). It also has poker-night extras: Spotify with lyrics, the cheatsheet, and the friend group's profit-and-loss.
- **Calibration** (in `webapp`, owner only) pulls frames from a table camera attached to the CV server.

## 3. Screen and route inventory

### `client/` (`src/routes.js`; everything except about and auth sits behind `ProtectedRoute`; `/` and `*` redirect to `/dashboard` or sign-in)

| Route | Layout | What it does | Status |
|---|---|---|---|
| `/authentication/sign-in` | `layouts/authentication/sign-in` | Login form | ✅ ("Remember me" does nothing) |
| `/authentication/sign-up` | `…/sign-up` | Register, then auto-login | ✅ (Facebook/Apple/Google buttons are `href="#"` ❌) |
| `/dashboard` | `layouts/dashboard` | `PokerGameBox` (table display) + "Win/Loss Watch" chart | 🟡 table works; chart ❌ (empty series, needs a `player.pnl` the backend never sends) |
| `/poker` | `layouts/poker` | Table display only: oval seating (`utils/positionUtils.js`), PNG cards, community-card flip animation, pot, dealer, fullscreen | 🟡 see bugs below |
| `/cheatsheet` | `layouts/cheatsheet` | 169-hand strength heatmap (AnyChart CDN, data from `/cheatsheet/heatmap`) + chip-distribution optimizer | ✅ (optimizer needs a Gurobi license) |
| `/analytics` | `layouts/analytics` | Line chart + player table | 🟡 fully hard-coded: friends' P&L copied from `Poker_Chip_Tracker.xlsx` by the Node script `data/ExtractExcel.js`; hard-coded "Online" badges; dead "Edit" link |
| `/spotify` | `layouts/spotify` + global `SpotifyMiniPlayer` | OAuth through the backend, Web Playback SDK player, synced lyrics through the backend | ✅ (needs Spotify Premium) |
| `/profile` | `layouts/profile` (805 LOC) | Edit username/email, avatar upload, password change, logout; forces re-login after a username change | ✅ |
| `/about` (public) | `layouts/about` | Team cards (6 members) + marketing copy | ✅ static |

Table-display bugs:
- **Jacks render as Aces.** `parseRankFallback` tests `includes('a')` before `jack` (confirmed by running it on `JACKH`).
- `probWin` is always 0.
- On any fetch error it silently swaps in demo data.

Template chrome: `examples/Sidenav` (SPADE wordmark + spade SVG), `DashboardNavbar` (fake "Type here..." search), `Configurator` (theme drawer whose links are Rickrolls).

### `webapp/` (`src/App.js`, page held in `useState`: no URLs, a refresh returns to home, no deep links)

| Page | File | What it does | Status |
|---|---|---|---|
| auth gate / `profile` | `pages/ProfilePage.js`, `components/auth/AuthForms.js` | Login/register when logged out; when logged in: edit, password, avatar upload, preset colour avatars, stats | 🟡 colour avatar is local only; Games/Wins stats read fields that don't exist (always 0); no re-login after a username change |
| `home`, not seated | `pages/LobbySystem.js`, `components/lobby/TableCard.js` | Public/all toggle, search, create-table modal (auto-joins at min buy-in), join modal with buy-in vs balance check | ✅ (fake "Pro tournament tonight" banner ❌; `alert()` stub for "Leave Table" ❌) |
| `home`, seated | `pages/HomePage.js` + `components/game/ActionPanel.js` + `CardScanner.js` | Table header (owner badge, delete, leave; disabled during a game); owner Start/End (needs ≥2 players); pot / to-call / your bet / chips; Raise/Call/Check/Fold | ✅ actions. 🟡 no All-in button; no view of opponents, board or winner |
| (seated) card scanner | `CardScanner.js`, `common/CamDiv.js` | Webcam frame every 500 ms sent to socket.io `frame`; detected cards shown hidden until tapped; confirm / retry | ❌ no server; confirmed cards are never sent to the backend |
| `calibration` (owner only) | `pages/CalibrationPage.js` | Table-camera frame every 1 s, "Recalibrate" button | ❌ no server |
| `debug` | `pages/GameDebug.js` | Raw game-state inspector (all hole cards, stage, auto-refresh) | ✅ dev tool |

Header: dark/light toggle (not persisted); the socket.io indicator is always red.

## 4. End-to-end flow today
1. On the phone (`webapp`): register, then log in. The JWT is stored in `localStorage.token`.
2. Lobby: create a table (`POST /tables`), which auto-joins at `minBuyIn` and moves money from balance to chips. Or pick a table and join with a buy-in.
3. Seated: STOMP CONNECT with the Bearer token, `SEND /app/game/{id}/connect`, `SUB /topic/tables/{id}`.
4. The owner presses Start (`bigBlind` is fixed at 20). The backend **deals virtual cards from its own `Deck`** (`session/RoundSession.java`).
5. Each WebSocket event makes the client re-fetch `GET /games/tables/{id}/status`. The player acts with `SEND /app/game/{id}/action {action, amount}`. Raise means **raise-by**, with a minimum of the big blind.
6. The hand ends. There is **no winner or showdown display**. The owner ends the game, then players leave (chips go back to balance) or the owner deletes the table.
7. In parallel, the TV (`client`), logged in **as a seated account**, polls `/players/current-table` and `/status` every 2s and renders the table.
8. Stats are not recorded anywhere (`/statistics` is archived on the backend).

## 5. API contract
Base URL is `REACT_APP_API_BASE_URL`. C = client, W = webapp.

### REST calls that work

| Method / path | Payload → response | Used by |
|---|---|---|
| POST `/users/register` | `{username,email,password}` → UserDto | C sign-up, W |
| POST `/users/login` | `{username,password}` → `{token,user}` | C, W |
| GET `/users/me` | → `{id,username,email,balance,isAdmin,avatarBase64}` | C AuthContext, W App |
| PUT `/users/me` | `{username,email}` (a new username invalidates the JWT) | C, W profile |
| PUT `/users/me/password` | `{currentPassword,newPassword}` | C, W |
| PUT `/users/me/avatar` | multipart field `avatar` | C, W |
| GET `/players/me` | → PlayerDto (chips, currentTableId, **embeds the full User entity**). Returns 404 before the first join | W (chips; also an unconditional side-call inside `getCurrentUser`) |
| GET `/players/current-table` | → `{isAtTable, tableId, table}` | C poll, W |
| GET `/tables`, `/tables/public`, `/tables/{id}` | → TableDto[] / TableDto | W lobby |
| POST `/tables` | `{name,description,maxPlayers 2–10,minBuyIn,maxBuyIn,isPrivate}` | W |
| POST `/tables/{id}/join?buyIn=` and `/leave`; DELETE `/tables/{id}` | | W |
| POST `/games/tables/{id}/start?bigBlind=` and `/end` | → `{success,message,gameState}` | W owner |
| GET `/games/tables/{id}/status` | → `{success, gameState}`; always 200 | C (2s poll), W, W debug. **Exposes every player's `holeCards` to any authenticated user** |
| GET `/cheatsheet/heatmap`; POST `/cheatsheet/chips/optimize` | ChipInventoryDto → `{maxPlayers,targetValuePerPlayer,distribution}` | C |
| GET `/spotify/login` (browser redirect), `/spotify/refresh_token`, `/spotify/lyrics` | | C. The callback redirects to the frontend `/spotify#access_token=…` |

### Dead or mismatched REST
- **Frontend calls with no backend** (defined in both `ApiService.js`, never invoked):
  - `/games/tables/{id}/my-cards`, `/cards/scan`, `/history`, `/rounds/{r}/history`, `/ai-hint`
  - `/statistics/*`, `/tournaments*`
  - `GET /players` (in `authorsTableData.isActive`, which is itself unused)
- **Backend endpoints with no UI:**
  - the whole `/api/friends/**` feature
  - `GET /users/{id}`, admin `PUT /users/{id}/role` and `/balance`
  - `/cheatsheet/chips/presets` and `save-preset` (a stub on the backend)

### STOMP (webapp only, `services/GameWebSocketService.js`; SockJS first, then a native WebSocket fallback)
- CONNECT header `Authorization: Bearer <jwt>` ✅
- SEND `/app/game/{id}/connect`, `/action` `{action:"CHECK|CALL|RAISE|FOLD", amount}`, `/disconnect` ✅. The backend also accepts `ALL_IN`, but no button sends it.
- SUB `/topic/tables/{id}` → `{type, timestamp, payload, message}`.
  - Webapp handles GAME_STARTED, GAME_ENDED, STAGE_CHANGED, PLAYER_TURN, PLAYER_ACTION.
  - The backend only emits GAME_STARTED, GAME_ENDED, PLAYER_ACTION, STAGE_CHANGED and PLAYER_CONNECTED/DISCONNECTED (`GameService.java`).
  - **PLAYER_TURN, COMMUNITY_CARDS_REVEALED and WINNER_DECLARED are never sent.**
- SUB `/user/queue/errors` 🟡: the backend sends with `convertAndSendToUser(sessionId, …)`, which targets a username. Errors such as "It's not your turn" almost certainly never arrive.

### socket.io (webapp only; target `http://localhost:8080` or the page origin, but Spring doesn't speak socket.io)
- `frame` `{n:2, image: ArrayBuffer JPEG}` → ack `{found, predictions:["AS","10H"]}`
- `getFrame` `{tableId}` → ack `{image: ArrayBuffer}`
- `recalibrate` `{tableId}` → ack `{success, message}`

**Card notation differs in three places:**
- CV service: `AS`, `10H`
- Backend: `Value.name()` + the suit's first letter, e.g. `JACKH`, `TENC`
- Client's own mapping table: `THREES` and similar

### Third-party calls from the browser
Spotify Web API and SDK (`sdk.scdn.co`), AnyChart CDN, Google Fonts, an unused Leaflet CSS link.

## 6. Template boilerplate vs product code (client, 17.1k JS/CSS LOC + a 2.1k-line JSON)
- **Template, ~7.1k (≈41%):**
  - `assets/theme` (2,872)
  - `components/Vui*` (1,269)
  - `examples/` (2,339)
  - `context/index.js` (106)
  - auth `components/` (470)
- **Product, ~10k:**
  - poker 2,424
  - Spotify 2,536
  - services + auth 1,107
  - analytics 860 (mostly data)
  - profile 805
  - cheatsheet 705
  - sign-in/up 644 (re-skinned template)
  - about 404
  - App + routes 329
  - dashboard 226
- **Unused files:**
  - `layouts/poker/data/{playersData,communityCardsData}.js`, `poker/utils/pokerPlaceholder.js`
  - `examples/Charts/BarCharts`, `examples/Icons/Spotify.js`, `authentication/components/Socials`
  - `cheatsheet/data/Heatmap.json` (the backend serves its own copy)
  - `analytics/data/ExtractExcel.js` (a Node script)
  - **all `spade-logo/*.png`** (only the README uses them) and all `poker_table/*.png`
  - about 20 template images: logos, visa/mastercard, PayPal QR, team-*/ivana etc.
  - In all, about 25 MB of images in `src/assets`, most of it unused.
- **Junk dependencies:** `components`, `images`, `install`, `ajv`, `anychart` (loaded from the CDN instead), `react-countup`, `react-flatpickr`, `stylis`, `@mui/styles`, `fork-ts-checker-webpack-plugin`.
- **`webapp`:** 8.2k LOC, all hand-written. `components/game/PokerTable.js` is an empty stub.

## 7. State, auth, errors, i18n, tests
- **State:**
  - client: React Context (the template's UI reducer, `AuthContext`, a 501-line `SpotifyContext`).
  - webapp: everything is `useState` in `App.js`/`HomePage.js` passed down as props. Table status is fetched twice (App and HomePage).
  - Neither uses a data-fetching library.
- **Auth:**
  - Both use the JWT in `localStorage["token"]` (the same key) with `fetch` and a Bearer header. There is no refresh or expiry handling.
  - client: a 401 clears the token and hard-redirects.
  - webapp: a 401 only clears the token, and the user (including the base64 avatar) is cached in `localStorage["pokerUser"]`.
  - Spotify tokens arrive in the URL fragment and are stored in localStorage.
- **Errors:** `console.error` plus transient strings cleared by `setTimeout`. No error boundaries; heavy debug logging left in (`STOMP Debug`, `ActionPanel Debug`).
- **i18n:** none. The UI is in English, many code comments are in German, and analytics shows €.
- **Tests:** none in either frontend; the testing-library packages are installed but unused. The backend has 2.

## 8. Visual design
- **client:** Vision UI dark glassmorphism.
  - Colours: navy `#0f1535` over `body-background.png`, accent `#0075ff`, purple gradients.
  - Font: "Plus Jakarta Display".
  - Brand: white spade SVG (`examples/Icons/SpadeLogo.js`) with the "SPADE" wordmark; page title "SpadeHub".
  - Table: CSS green-felt oval with PNG cards.
- **webapp:**
  - Colours: Tailwind-like grays (`#111827`) with a purple→blue gradient (`#6a11cb → #2575fc`).
  - Theming: CSS variables with a dark/light switch.
  - Logo: a GiPokerHand icon, **not the spade**, so the brand is inconsistent between apps.
- **Worth keeping:**
  - Spade marks: `spade_logo_rectangle.png` (white ribbed spade on black), `spade_assistant_1920x1080.png` (chrome spade with chip stacks), `public/spade_logo.svg`, and the SVG path in `SpadeLogo.js`.
  - The full 52-card PNG deck plus backsides and `DealerButton.png`.
  - The chip colour scale (`ChipDistributionCard.js`).
  - The seat-placement maths in `positionUtils.js`.

## 9. Hygiene (`git ls-files`)
- **Clean:** `client/build` has 0 tracked files, `webapp/build` 0, `node_modules` 0. All three exist locally and are ignored.
- **Tracked despite `*.pem` in `.gitignore`:** `client/{cert,key}.pem` and `webapp/{cert,key}.pem` (force-added in commit `27329b6`). That is a **private key in a public repo**. The working copies are re-generated, uncommitted, and the same key as the backend keystore; they should not be committed.
- **Secrets in history:**
  - `spadeboot/gurobi.lic` was added in `3f25aee` and removed in `358da91`, so the **license is in master history**.
  - Branch `origin/add_first_game_logic` (commit `0d50468`, "Strong secret") contains `application.yml` with a **DB password and JWT secret**.
- **Other:**
  - The `.env.*` files are tracked but hold only URLs (fine).
  - 7 `.DS_Store` files are tracked.
  - `client/package-lock.json` is gitignored, so client builds aren't reproducible.
  - Friends' photos and nicknames sit in a public repo.
- **Uncommitted chip change:** `ChipDistributionCard.js` adds a `$20` chip (colour, default 0, input row). It pairs with the backend `ChipInventoryDto.chip20` and a `CheatsheetService` switch from Gurobi's cloud license to a local license file. This is coherent and complete across all three files.

## 10. Keep vs throw away
**Keep (as knowledge, contracts or assets):**
- The REST, STOMP and socket.io contracts above.
- Domain rules:
  - bankroll → buy-in → table chips; one table at a time
  - owner can start (≥2 players), end and delete; leave and delete are blocked during a game
  - raise = raise-by, minimum = big blind
  - to-call = currentBet − my currentBet; check allowed only when that is 0
  - a username change invalidates the JWT
- The privacy pattern for hole cards (hidden until tapped, then confirm or retry).
- The table-display layout: seat-oval maths and the flip animation.
- The cheatsheet heatmap and chip optimizer.
- The Spotify flow (backend OAuth, Web Playback SDK, lyrics).
- The Spotify callback route `/spotify` with hash tokens, which is hard-wired in the backend.
- The brand assets listed in §8.

**Throw away:**
- The Vision UI theme, Vui* components, `examples/`, Configurator, fake search and fake social login.
- Hard-coded analytics data and the empty Win/Loss chart.
- Demo-data fallbacks, dead `ApiService` methods, the fake tournament banner.
- The state-based "router", duplicated `ApiService`/`environment.js`, junk dependencies and unused images.
- `genezio.yaml`.

## 11. Open questions for the owner
1. **Source of truth for cards:** physical cards read by CV, or virtual cards dealt by the server? Today the server deals its own deck and the scanner never feeds the engine.
2. **Where is the CV/socket.io service** (`github.com/lucabzt/Spade`, Flask on :5000?), and should the rebuild keep the `frame` / `getFrame` / `recalibrate` contract or route CV through Spring?
3. **One app with roles** (player phone / table TV / owner admin) or keep two apps?
4. **Should the TV view show every hole card?** If not, `/status` must stop sending `holeCards` to everyone.
5. **Keep the SpadeHub extras?** Spotify (Premium only), the cheatsheet (Gurobi license cost), the friend-group P&L.
6. **Friends feature and admin balance/role endpoints:** build a UI or drop them? The same goes for stats, history, AI hint and tournaments.
7. **Configurable big blind and all-in;** where should winner and showdown be shown, since the backend needs to emit those events first?
8. **Domain layout:** `hub.poker-spade.de` vs `poker-spade.de` — which app goes where?
9. **Rotate and purge** the committed TLS key, Gurobi license, JWT secret and DB password?