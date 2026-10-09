# Legacy API contracts

What the old services said to each other, so the rebuild knows what it keeps or drops. Three generations: the 2024 Python server, the 2025 Spring backend (this repo at V0), and the card-detection service.

## 1. Python Flask server (`lucabzt/Spade@1510db9:server/app.py`, https `127.0.0.1:5000`)
One hard-coded 8-player round (SB 10 / BB 20) in a background thread; actions were queued.

| Route | What it did |
|---|---|
| `GET /players` | `[{name, balance, pnl: [[ts, pnl]…], bet, cards: [{rank, suit, faceUp}], probWin, folded, actionPending}]` |
| `GET /community-cards` | 5 cards, padded with `{rank: null, suit: null, faceUp: false}` |
| `GET /dealer`, `GET /pot` | `{dealerIndex}`, `{pot}` |
| `POST /place-bet`, `/fold`, `/player-action` | `{playerName, amount}` / `{action: "fold" \| "check" \| "call" \| "raise N"}` |
| `POST /next-round`, `/reset-game` | advance or reset |
| `GET /login`, `/callback`, `/refresh_token`, `/lyrics` | Spotify OAuth and Genius lyrics (ported to Spring) |
| `GET /heatmap` | 169 starting hands (ported to Spring) |
| socket.io `frame` | YOLO inference (moved to spadeAI, now `cv/`) |

Informal notes: `lucabzt/Spade@1510db9:server/Endpoints.txt` (German).

## 2. Spring backend at V0 (`spadeboot/`)
The full REST and STOMP surface, derived from the controllers, is in [status-quo/backend.md](../status-quo/backend.md#api-surface-derived-from-the-controllers-at-head). Re-derive it with:
`grep -rn "@\(Get\|Post\|Put\|Delete\)Mapping\|@RequestMapping\|@MessageMapping" spadeboot/src/main/java`.

Response shapes worth knowing:
- `POST /api/users/login` → `{token, user}`; `GET /api/users/me` → `{id, username, email, balance, isAdmin, avatarBase64}`.
- `GET /api/players/current-table` → `{isAtTable, tableId, table}`.
- `GET /api/games/tables/{id}/status` → `{success, gameState}` (always HTTP 200; **includes every player's `holeCards`**).
- STOMP `/topic/tables/{id}` → `{type, timestamp, payload, message}`; sent types: `GAME_STARTED`, `GAME_ENDED`, `PLAYER_ACTION`, `STAGE_CHANGED`, `PLAYER_CONNECTED`, `PLAYER_DISCONNECTED`.
- STOMP `/app/game/{id}/action` ← `{action: "CHECK" | "CALL" | "RAISE" | "FOLD" | "ALL_IN", amount}`; raise means *raise by*.

**Frontend calls that never had a backend:** `/games/tables/{id}/my-cards`, `/cards/scan`, `/history`, `/rounds/{id}/history`, `/ai-hint`, `/statistics/*`, `/tournaments*`, `GET /players`. **Events the frontend waited for that were never sent:** `PLAYER_TURN`, `COMMUNITY_CARDS_REVEALED`, `WINNER_DECLARED`.

## 3. Card detection (`cv/`, from `lucabzt/spadeAI@9e4ec5e`)
socket.io on `:5001`, answers through the ack callback. Full table in [`cv/README.md`](../../cv/README.md#contract-socketio-answers-via-the-ack-callback): `frame`, `comm_cards` (stub), `getFrame`, `recalibrate`.

## Card notation today
Three formats, to be unified in V1:
- **cv:** rank + suit letter, `AS`, `10H`, `QD`.
- **backend:** `Value.name()` + suit initial, `ACEH`, `JACKH`, `TENC`.
- **legacy client:** its own mapping table, `THREES`-style.
