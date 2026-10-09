> **Raw audit, 2026-10-09.** Read-only subagent report, kept as evidence for [the V0 spec](../V0_2026-10-09_FOUNDATION.md). Not maintained: Phase 04 turns it into `docs/status-quo/`. Paths and line numbers were true on the audit date.

# Spade predecessor repos audit (read-only)

**Bottom line:** `lucabzt/Spade` is the original project. The current SPADE repo is a new Spring Boot backend plus the evolved React frontends from that original. Most of the Python era did not make it across: the hand-evaluation tests, the win-probability engine, the 436-clip voice library, the CSV game logs and the design notes. In the current repo, card scanning and camera calibration probably don't work: the webapp opens its socket.io connection to Spring Boot on :8080 instead of the CV service on :5001, and I found no socket.io server in spadeboot. I confirmed this by reading the code, not by running it.

All repos are left as I found them. One cleanup: my `git fsck --lost-found` created `/Users/sebastianrogg/IdeaProjects/SPADE/.git/lost-found/` (marker files only), and I deleted it again. I also made a few read-only GitHub calls to check remote branches and history.

## A. Timeline and lineage

| When | Repo | What happened |
|---|---|---|
| Oct 24 2024 to May 22 2025 | `~/PycharmProjects/Spade` (lucabzt/Spade), 476 commits | Original project. Python game engine (Nov 2024), Flask `server/app.py` (Dec 1), React `client/` (Dec 3), YOLO model (Dec 23), Spotify (Dec 30), lyrics (Jan 13), xlsx (Jan 15), phone `webapp/` (Feb 23), UML and z-files (Mar 12–13), webapp wired to Spring `https://localhost:8080/api` (Mar 27 to Apr 9). The remote has 2 more commits than local (README only, Jul 21 2025). |
| Mar 10 2025 | `SEBA/SpadeBoot` (M4RKUS28/SpadeBoot), 13 commits | A university course template (Student/Lecture/Backpack/Device entities) renamed to `de.poker.spade`. It contains no poker logic. |
| Mar 12 2025 | `SEBA/SpringSpadeBoot` and `SEBA/SPADE` | The same repo cloned twice (eixfachZabii/SpringSpadeBoot, identical HEAD `60f8c33`). The `com.pokerapp` code is pasted straight from the AI chat transcript in `Spade/zDone`. |
| Mar 13–24 2025 | eixfachZabii/SpadeBoot, branch `add_first_game_logic` (59 commits) | First real Spring backend: JWT, roles, `HandEvaluation`, 22-test `HandEvaluatorTest`. |
| Mar 14–24 2025 | `spadeAI` (lucabzt/spadeAI), 14 commits | Card-detection (CV) service split out of the Flask server. |
| Jul 21–26 2025 | current SPADE `master`, 17 commits | Starts from a fresh root, "Initial commit from local SPADE project". It shares no history with `add_first_game_logic`. |

- **Repo names:** GitHub `eixfachZabii/SpadeBoot` and `eixfachZabii/Spade` show identical branch SHAs, so it is one repo that was renamed.
- **Gap in history:** the remote has no commits between Mar 25 and Jul 20 2025. About four months of backend work (sessions, friends, Spotify and cheatsheet ports) has no history anywhere I could find.
- **Archived mirror:** `eixfachZabii/Spade-archived-` did not show up in a GitHub search, so it may be private. I didn't verify it.
- **Hackathons copy:** `~/PycharmProjects/Hackathons & Projekte/SPADE/client` is byte-identical (all 122 files) to the original "spade-ui" client in Spade commits `2b1ad0e` to `ed6e3c3` (Dec 30–31 2024). On Dec 31 (`4c6a9c3`) that client was replaced by the Vision-UI "client-hub". It is a stale copy, recoverable from git; only its `build/` and `node_modules` (489 MB) are extra.
- **Team (git author names):**
  - In lucabzt/Spade: Sebastian Rogg / sebastianrogg (eixfachZabii, about 300 commits under 4 emails), Markus (M4RKUS28, 98), Luca Bozzetti (lucabzt, 76), Matthias Meierlohr (1), paulv (1).
  - On the March Spring branch: JonasHoerter (3).
  - A separate training repo, `lucabzt/SpadeClassifier`, exists only on GitHub.
- **Superset or rewrite?**
  - **Frontends:** current `client/` and `webapp/` evolved from Spade's. The webapp adds four files (GameDebug, a WebSocket service, env config, debug CSS). The client drops 50 files (Billing/PayPal, `GameButtons`, unused template parts) and adds 6.
  - **Backend:** a rewrite. The Python server is entirely gone. `HandEvaluation.java` matches the March branch exactly apart from the package rename.

## B. Original Python server (`Spade/server/app.py`, Flask on https 127.0.0.1:5000)

The server ran one hard-coded 8-player `GameRound` (SB 10 / BB 20) in a background thread. `play_round()` blocked on a shared `Queue` of player actions.

| Route | What it did | Status in current spadeboot |
|---|---|---|
| `GET /`, `/index.html` | "Software Spade" landing page (`templates/index.html`, 118 lines, three feature cards) | Lost (the React About page plays a similar role) |
| `GET /players` | `[{name, balance, pnl:[[ts,pnl]...], bet, cards:[{rank,suit,faceUp}], probWin, folded, actionPending}]` | Partly survives in `/api/players/me` and the game-state DTO. `Player.winProbability` exists but nothing ever sets it. Per-player PnL time series: **lost** |
| `GET /community-cards` | 5 cards, padded with `{rank:null,suit:null,faceUp:false}` | Folded into game state |
| `GET /dealer`, `GET /pot` | `{dealerIndex}`, `{pot}` | Folded into game state |
| `POST /place-bet`, `/fold`, `/player-action` | `{playerName, amount}` / `{action:"fold"\|"check"\|"call"\|"raise N"}` pushed onto the queue | Replaced by STOMP `/game/{tableId}/action` |
| `POST /next-round`, `/reset-game` | Advance or reset the round | Replaced by `/api/games/tables/{id}/start\|end\|status` |
| `GET /login`, `/callback`, `/refresh_token` | Spotify OAuth (callback redirects to `https://127.0.0.1:3000/spotify/#access_token=…`) | Survives as `/api/spotify/login\|callback\|refresh_token` |
| `GET /lyrics?artist&title` | Genius lookup with filtering of section headers and "embed" lines | Survives as `/api/spotify/lyrics` |
| `GET /heatmap` | 169-cell starting-hand heatmap (`assets/data/HeatMapData.py`) | Survives verbatim in `CheatsheetService` (`/api/cheatsheet/heatmap`) |
| socket.io `frame` | YOLO inference inside Flask | Moved to spadeAI |

Other notes on the Python server:

- **Lyrics.py:** a standalone CLI that splits Genius lyrics into evenly timed "karaoke" chunks. The idea never shipped.
- **Endpoints.txt:** an informal contract in German notes that matches the field shapes above.
- **Broken Docker setup:** the Dockerfile runs `uvicorn main:app`, but there is no `main.py`. `docker-compose.yml` maps port `433`, which looks like a typo for 443.
- **Committed secrets in public history:**
  - in `app.py`: Spotify client ID and secret, Genius token, Flask secret key
  - in `classifier/inference.py`: a Roboflow API key
  - TLS `cert.pem`/`key.pem` files
  - The current `spadeboot/src/main/resources/application*.yml` reuses the same Spotify client ID and has a newer secret and Genius token in plain text. All of these should be rotated.

## C. spadeAI CV service contract

- **Transport:** Flask-SocketIO with `async_mode='eventlet'`, CORS `*`, `0.0.0.0:5001`. It runs plain HTTP: `CERT_FILE`/`KEY_FILE` (`./certificates/*.pem`, git-ignored) are defined but never passed to `socketio.run`. `eventlet` is also missing from `requirements.txt`. There are no HTTP routes; everything is socket.io events that answer through the ack callback.
- **`frame`:** request `{n:int, image:ArrayBuffer (JPEG)}`, response `{predictions:[label...], found: len>=n}`. Detected labels are de-duplicated (the same card is often detected twice, once per corner) and cut to `n`.
- **`comm_cards`:** request `{n}`. It is a **stub** that always returns `["QS","AS","KS"]`. Community-card detection was never built.
- **`getFrame`:** returns `{success, image: JPEG bytes}`, the cropped overhead frame with the detected spades boxed in red.
- **`recalibrate`:** returns `{success}`. It loops until it finds two or more red regions (HSV red thresholds), then crops from the left spade to the right spade plus 5% padding. It also detects yellow 20–50 px outlines (the card slots) but does nothing with them.
- **Camera assumptions:**
  - An overhead webcam at `cv2.VideoCapture(0)` on the spadeAI host, looking at a Texas Hold'em mat whose 5 yellow card boxes sit between two red ♠ symbols. The reference photos in `Spade/server/src/classifier/table/images/` show this.
  - Player hole cards come from the phone camera in the webapp (`CardScanner.js`: canvas to JPEG at 0.8 quality, `emit("frame",{n:2,…})`).
  - On phones the camera API only works on HTTPS pages, but spadeAI serves plain HTTP. That mismatch is a likely source of mixed-content errors.
- **Model:** `models/best_60_23.pt`, 22.6 MB, **committed** (an identical copy is in `Spade/webapp/models/`).
  - YOLOv8s fine-tuned for 60 epochs, imgsz 640, batch 16, ultralytics 8.3.78, trained 2025-02-23.
  - 52 classes named rank+suit (`2C`…`10S`, `JD`, `QH`, `KS`, `AC`).
  - Dataset path: `/home/bozz_lu/Code/other/SpadeClassifier/data/data.yaml` (the SpadeClassifier repo, not local).
  - Earlier experiments are visible in Spade history: PyTorch classifiers (`model_80_/99_.pt`, Nov 2024, deleted) and Roboflow `playing-cards-ow27d` v4.
- **Who calls it:**
  - In lucabzt/Spade HEAD, the webapp connected with `io("http://localhost:5001")`.
  - In the **current** SPADE, `webapp/src/App.js` connects socket.io to `http://localhost:8080` / `window.location.origin` with reconnection turned off. Spring has STOMP only and no socket.io server, so `frame`/`getFrame`/`recalibrate` presumably fail silently.
  - Nothing calls `comm_cards`.

## D. Poker game logic across the repos

| Location | Contents | Assessment |
|---|---|---|
| `Spade/server/src/game/` (`game_round.py` 304 lines, `betting_round.py`, `hand_analysis/*`, `utils/game_utils.py`) | Blinds, preflop/flop/turn/river, last-raiser loop, fold-out win, showdown, even split pot, PnL matrix, CSV logs | Bugs: dealer/SB rotation is inconsistent, raise sizing is "raise by", no all-in, **no side pots**, no min-raise. Its main value is the **40 unit tests** (`server/src/tests/test_analysis/`, including kicker and 4-way split cases), which are ready-made test vectors. |
| `Spade/server/src/engine/` | Vendored Holdem Calculator (Kevin Tseng, MIT): Monte Carlo and exact equity, multiprocess | The only equity / win-probability implementation that ever worked. Lost. |
| `Spade/server/src/mediaplayer/sound_manager.py` and `server/assets/sounds/` | 436 pre-recorded ElevenLabs "Daniel" voice clips (30 MB): street calls, player names, action lines, hand descriptions such as "full house, kings over…", winner lines | The real "voice dealer" (it plays recorded clips, there is no live text-to-speech). Several folders still have `TODO` placeholder clips, and two-pair announcements were never finished. **Nothing of this exists in current SPADE.** |
| `SEBA/SpringSpadeBoot` and `zDone` | AI-generated `HandEvaluator` (78 lines) and `GameServiceImpl` (432 lines) | Scaffold quality. Has ALL_IN status but no side pots. |
| `add_first_game_logic` branch | `HandEvaluation.java` and `HandEvaluatorTest.java` (22 tests) | The evaluator went to master; **the test file did not**. Master has only `GameServiceTest` (9 tests). |
| current `spadeboot` `RoundSession`/`GameSession` | Correct heads-up blind rule, all-in handling | `pot / winners.size()` with no side pots and no remainder chip handling. |

**Best implementation:** for the rebuild, use the current `spadeboot/.../domain/game/HandEvaluation.java` as the evaluator. Restore the 22 Java tests from `origin/add_first_game_logic:src/test/java/com/pokerapp/HandEvaluatorTest.java` and port the 40 Python cases as extra fixtures. Side pots and equity have to be written new; the vendored `holdem_calc` is a good reference for the equity math.

## E. Design and planning artefacts (Spade root, all tracked)

- **`UML`:** a single PlantUML text file, not a directory (commit "UML Diagramm für KRasse AI", Mar 12). Classes: User (balance, avatar, buyChips, chooseRole), Player (winProb, buyIn, rebuy, invitePlayer, makeMove), Spectator (viewWinOdds, watchReplay), Game, GameRound (pot, pnlMatrix, `receiveCards()` from the CV "API"), BettingRound, Card (community/private, showing), HandRanking, ReplaySystem, LobbySystem, Stats, Database. Table size is 2–10 players.
- **`zUML.txt`:** a refined PlantUML class diagram plus a second **layered architecture diagram** (Presentation / Application / Domain / Infrastructure).
  - New types: `PlayerStatus{ACTIVE,FOLDED,ALL_IN,SITTING_OUT}`, `BettingStage`, `MoveType{CHECK,CALL,RAISE,FOLD,ALL_IN}`, Table, Deck, Hand.
  - Service interfaces: UserService, GameService, HandEvaluationService.
- **`zFolder.txt`:** the target `com.pokerapp` package tree. It includes JWT security, `WebSocketConfig`, Flyway `V1__init_schema.sql`, and tests such as `PokerGameFlowTest`.
- **`zDone`:** 3,624 lines. It is a raw copy of a SiemensGPT chat with Claude 3.7 Sonnet, including the Siemens AI-usage disclaimer, so watch IP/provenance before reusing its code. Contents:
  - The full generated `com.pokerapp` code: REST under `/api/users|games|tables|invitations|replays|statistics`, STOMP `/games/{id}/move` and `/chat` going to `/topic/games/{id}`, MySQL config, and a hard-coded JWT secret.
  - **Eight user stories**, the closest thing to a requirements spec: login with balance; create a game; join with part of your balance; spectate; invite players; bet/fold/raise/check; leave with remaining chips returned to balance; log every game for replay and fairness review.
- **`zBackend.txt`:** a `CommandLineRunner` seed of users, players, spectators, 3 tables (beginner, pro, private invite-only), games, replays, results, statistics and invitations (PENDING/ACCEPTED/DECLINED).
- **Status of those features today:** Invitation, Replay and Statistics are commented out under `api/controller/archiv/`. Friends replaced Invitations.
- **`Poker_Chip_Tracker.xlsx`:** byte-identical to `spadeboot/src/main/resources/`, where no Java code references it.
  - It is the group's real home-game ledger (created 2024-10-13, last edited 2025-05-21).
  - Sheets: `Day 1`…`Day 20` and `DayTemplate`, each a table of player, buy-in ("Total Bet", €), and counts of 5/10/25/100-point chips. The formula `WIN/LOSS = Σ(chips × 0.05/0.10/0.25/1.00) − buy-in` and the Σ column check that each night's total is zero.
  - `Overview` has per-day win/loss by VLOOKUP, a cumulative "MOVING SUM" table, and a line chart.
  - This is the source of the client's Analytics/"Win/Loss Watch" page. It is baked in by `client/src/layouts/analytics/data/ExtractExcel.js`, which expects the xlsx at the repo root. In the current repo that path no longer resolves.
  - It is **not** the chip-distribution optimizer (`/api/cheatsheet/chips/optimize`); that is separate logic.

## F. Product vision signals

- **READMEs (Spade, SpadeBoot):** "AI poker dealer… James Bond / Casino Royale", card detection, action tracking, TTS winner calls, "AI Strategy Insights (Future Feature)", "Effortless Setup". "Action tracking" by deep learning was claimed but never built; actions always came in by button or API.
- **spadeAI README:** community cards from the overhead camera, player cards from smartphone photos. It links a `github.com/spade-poker` org.
- **Flask landing page:** "Real-Time Insights / Comprehensive Analytics / AI-Powered Assistance".
- **Other signals:** the voice and personality assets (a humor folder, greetings by player name), the Spotify DJ, lyrics/karaoke, PayPal QR settle-up (Billing page, dropped later), and the starting-hand cheatsheet.
- **No pitch deck or hackathon material** exists in any of these repos.

## G. Reference assets worth carrying into `docs/references/`

1. `~/PycharmProjects/Spade/UML` and `zUML.txt`: PlantUML domain model and layered architecture.
2. `~/PycharmProjects/Spade/zFolder.txt`: the intended package structure.
3. The user stories from `~/PycharmProjects/Spade/zDone` (lines 469–476). Extract only these, not the generated code.
4. `~/PycharmProjects/Spade/zBackend.txt`: a seed-data scenario (tables, invitation states).
5. `~/PycharmProjects/Spade/server/Endpoints.txt` and the route table in B: the legacy API and payload shapes.
6. A spadeAI contract doc written from section C; source files `~/PycharmProjects/spadeAI/app.py`, `camera.py`, `utils.py`.
7. `~/PycharmProjects/spadeAI/models/best_60_23.pt`. Keep it out of git (Git LFS or a release artifact) and document the 52-class label list.
8. `~/PycharmProjects/Spade/server/src/classifier/table/images/`: table and mat reference photos for CV fixtures. They are phone screenshots that include a strip of personal photo thumbnails, so crop them before committing.
9. `~/PycharmProjects/Spade/Poker_Chip_Tracker.xlsx`: ledger, chip values and P&L formulas.
10. `~/PycharmProjects/Spade/server/assets/data/HeatMapData.py`: starting-hand heatmap data.
11. Hand-evaluation test vectors:
    - `~/PycharmProjects/Spade/server/src/tests/test_analysis/*.py`
    - `git show origin/add_first_game_logic:src/test/java/com/pokerapp/HandEvaluatorTest.java` (run in `/Users/sebastianrogg/IdeaProjects/SPADE`)
12. `~/PycharmProjects/Spade/server/src/engine/` (MIT, keep the LICENSE): reference for the equity calculation.
13. `~/PycharmProjects/Spade/server/assets/sounds/` plus a manifest of its categories: the voice-dealer asset library (30 MB, not for the main repo).
14. `~/PycharmProjects/Spade/README.md` and the spadeAI README: vision text.

**Can be dropped:** `SEBA/SpadeBoot` (course template), the duplicate `SEBA/SPADE` / `SpringSpadeBoot` clones (their content is in `zDone`), and the Hackathons client copy.