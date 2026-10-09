# Frontends (`client/`, `webapp/`): status quo, 2026-10-09

> Dated snapshot, not maintained (V0 spec §5). Both apps are **frozen** and will be replaced in V1 by a new web hub and a native iOS player app ([ADR 0005](../adr/0005-rebuild-dont-repair.md)).
> **Changed since:** Phase 01: their TLS dev certificates are no longer tracked (generate them with the command in `USAGE.md`).
>
> Evidence: [audit](../handoffs/V0_audits/audit-frontends.md). Salvage verdicts: [salvage.md](salvage.md).

## The two apps
| | `client/` ("SpadeHub") | `webapp/` ("Spade") |
|---|---|---|
| Role | the **hub**: big shared screen showing the table, plus poker-night extras | the **player's phone**: lobby, betting, hole-card scanner |
| Base | Creative Tim × Simmmple **Vision UI Dashboard React** template, React 17, MUI 5, CRA | plain CRA, React 19, hand-written CSS, **no router** (page in `useState`) |
| Realtime | none: polls REST every 2 s | STOMP (`@stomp/stompjs` + SockJS) and socket.io |
| Look | Vision UI dark glassmorphism (navy `#0f1535`, accent `#0075ff`) | Tailwind-like grays, purple→blue gradient, dark/light theme ([style reference](../references/webapp-style.md)) |
| Code | ~41% template boilerplate | all hand-written |

Both read `REACT_APP_API_BASE_URL` / `REACT_APP_WS_URL` from tracked `.env.development` / `.env.production`, keep the JWT in `localStorage["token"]`, and run over HTTPS with a self-signed cert (`run.sh`).

## Routes
**`client/`** (`src/routes.js`):

| Route | What | Status |
|---|---|---|
| `/authentication/sign-in`, `/sign-up` | login, register | ✅ (social buttons are dead links) |
| `/dashboard` | table display + "Win/Loss Watch" chart | 🟡 chart is empty (`player.pnl` never sent) |
| `/poker` | table display: seat oval, PNG cards, board flip animation, pot, dealer, fullscreen | 🟡 Jacks render as Aces; demo data on fetch errors; shows **every** hole card |
| `/cheatsheet` | 169-hand heatmap + chip-distribution optimiser | ✅ (optimiser needs Gurobi) |
| `/analytics` | P&L chart + player table | 🟡 data hard-coded from the ledger spreadsheet |
| `/spotify` | OAuth via backend, Web Playback SDK, synced lyrics | ✅ (Spotify Premium only) |
| `/profile` | edit name/email, avatar, password, logout | ✅ |
| `/about` | team cards and marketing copy | ✅ static |

**`webapp/`** (`src/App.js`, no URLs):

| Page | What | Status |
|---|---|---|
| auth gate / profile | login, register, profile, colour avatars | 🟡 stats always 0 |
| lobby | public/all tables, search, create-table and join modals with buy-in check | ✅ |
| seated home | table header, owner start/end, pot / to-call / stack, Fold/Check/Call/Raise | ✅ (no All-in; no view of opponents, board or winner) |
| card scanner | webcam frame every 500 ms to socket.io `frame`, hidden-until-tapped result, confirm/retry | ❌ socket.io points at Spring, which has no socket.io server |
| calibration (owner) | table-camera frame + recalibrate | ❌ same reason |
| debug | raw game-state inspector | ✅ dev tool |

## End-to-end flow (as it works today)
Phone: register → lobby → create or join a table (buy-in moves bankroll to stack) → STOMP connect → owner starts (big blind fixed at 20; the server deals a virtual deck) → each event triggers a `/status` re-fetch → players act → **no showdown or winner is ever shown** → owner ends → players leave (stack back to bankroll). The hub, logged in as a seated account, polls the table.

## API contract and mismatches
- Working calls: everything listed in [backend.md](backend.md) under users, players, tables, games, cheatsheet, spotify.
- **Calls with no backend:** `/games/tables/{id}/my-cards`, `/cards/scan`, `/history`, `/rounds/{id}/history`, `/ai-hint`, `/statistics/*`, `/tournaments*`, `GET /players`.
- **Backend with no UI:** the whole `/api/friends/**` feature, admin role/balance, chip presets.
- STOMP: the webapp handles `PLAYER_TURN`, `COMMUNITY_CARDS_REVEALED`, `WINNER_DECLARED`, which the backend never sends. Errors on `/user/queue/errors` likely never arrive.
- STOMP origins allow only `:3000`, so whichever app runs second (`:3001`) cannot connect.
- Card notation differs in three places: cv `AS`/`10H`, backend `JACKH`-style, client mapping `THREES`-style.

## Known legacy bugs (recorded, not filed: the apps are being replaced)
- Jacks render as Aces (`parseRankFallback` tests `'a'` before `jack`).
- Empty Win/Loss chart; hard-coded "Online" badges; fake search bar; Configurator links are Rickrolls.
- The webapp's "Leave Table" in one place is an `alert()` stub; colour avatars are local only; dark/light choice not persisted.
- Two copies of `ApiService.js` and `config/environment.js`; heavy debug logging left in.

## Worth keeping (details in [salvage.md](salvage.md))
- The page **concepts**: table display, cheatsheet, analytics/ledger, music, profile; the phone's lobby → seat → act flow.
- The seat-oval maths (`client/src/layouts/poker/utils/positionUtils.js`), the chip colour scale (`client/src/layouts/cheatsheet/components/ChipDistributionCard.js`), the 52-card PNG deck, backs and dealer button, the Spade logo assets.
- The webapp's style and the hole-card privacy pattern (hidden until tapped, then confirm or retry).

## Throw away
The Vision UI theme and `Vui*` components, `examples/`, Configurator, fake search and social login, hard-coded analytics, demo-data fallbacks, dead `ApiService` methods, the state-based router, `genezio.yaml`, about 25 MB of unused template images.
