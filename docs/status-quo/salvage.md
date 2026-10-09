# Salvage map: what V1 takes from the old code

> V1 rebuilds Spade instead of repairing it ([ADR 0005](../adr/0005-rebuild-dont-repair.md), V0 spec D21). This map decides, per piece, what survives. It is input for the V1 grill, which may overrule any row; a changed verdict is recorded there, not here.
>
> **Verdicts:** **keep as-is** (use unchanged) · **keep, adapt** (port it, adjust it) · **concept only** (the idea or flow survives, the code does not) · **rewrite** (needed, but the code is not worth porting) · **drop** (not needed).

## Backend (`spadeboot/`)
| Piece | Path | Verdict | Why |
|---|---|---|---|
| Hand evaluator + its 18 test vectors | `domain/game/HandEvaluation.java`, `src/test/.../HandEvaluationTest.java` | **keep, adapt** | pure function, correct since V0 Phase 03; the vectors become the rebuild's spec. Adapt: one card notation, no in-place sorting of the caller's list |
| Card, suit and rank types | `domain/card/{Card,Suit,Value}.java` | **keep, adapt** | the vocabulary is right; `Card` stops being a JPA entity |
| Lobby rules: bankroll → buy-in → stack; one table at a time; owner starts (≥2 players), ends, deletes; leave and delete blocked mid-game; raise is raise-by | `service/TableService.java`, `service/GameService.java` | **concept only** | the rules are the group's real rules; the code mixes them with persistence bugs |
| Auth: register/login, BCrypt, JWT, `/me`, avatar | `security/`, `service/UserService.java`, `api/controller/UserController.java` | **keep, adapt** | works; adapt to a stable subject (user id, not username), explicit DTOs, WealthWatcher-style layering |
| Hold'em engine (threads, latches) | `session/` | **rewrite** | ADR 0002 needs a state machine fed by card reads; see the defects in [backend.md](backend.md) |
| Game/Round/Stage/Turn/Deck entities | `domain/game/`, `domain/card/Deck.java` | **rewrite** | never persisted; designed for a server-dealt deck |
| STOMP event publishing | `websocket/` | **concept only** | realtime table updates are needed; these events were never sent, and auth is missing |
| Heatmap data (169 starting hands) | `service/spadehub/CheatsheetService.java` | **keep as-is** | static, correct data |
| Chip optimiser | `CheatsheetService.java` (Gurobi) | **concept only** | the feature is liked; a commercial solver for a small integer problem is not |
| Spotify OAuth + lyrics | `service/spadehub/SpotifyService.java` | **concept only** | keep-or-drop is an open issue |
| Friends | `service/FriendService.java` | **concept only** | no UI ever used it; open issue |
| Secret-free config, fail-fast secret, dev seeding | `application.yml`, `JwtUtils`, `DataInitializer` | **keep as-is** | written in V0 |
| Commented-out `archiv/` controllers, unused dependencies | `api/controller/archiv/`, `pom.xml` | **drop** | dead |
| Docker setup | `Dockerfile`, `docker-compose.yml` | **rewrite** | broken bind/port, ARM-only; hosting is a V1 decision |

## Hub (`client/`)
| Piece | Path | Verdict | Why |
|---|---|---|---|
| The pages as concepts: table display, cheatsheet, analytics/ledger, music, profile, about | `src/layouts/*` | **concept only** | the owner keeps the dashboard and its pages as the concept (D23) |
| Seat-oval placement maths | `src/layouts/poker/utils/positionUtils.js` | **keep, adapt** | small, works, saves re-deriving the geometry |
| Board flip animation | `src/layouts/poker/` | **concept only** | the moment matters; rebuild it with the new stack |
| Chip colour scale | `src/layouts/cheatsheet/components/ChipDistributionCard.js` | **keep, adapt** | matches the physical chips |
| 52-card PNG deck, card backs, dealer button | `src/assets/images/` | **keep as-is** | ready-made assets (origin unverified; check before publishing) |
| Spade logo assets | `src/assets/images/spade-logo/`, `public/spade_logo.svg`, `examples/Icons/SpadeLogo.js` | **keep as-is** | the brand mark |
| Vision UI theme, `Vui*` components, `examples/`, Configurator | `src/assets/theme`, `src/components`, `src/examples` | **drop** | template |
| Hard-coded analytics data | `src/layouts/analytics/data/` | **drop** | the ledger replaces it |
| Auth and Spotify contexts | `src/context/` | **rewrite** | new stack |

## Phone (`webapp/`), to be replaced by the iOS player app
| Piece | Path | Verdict | Why |
|---|---|---|---|
| Visual style: tokens, gradient, dark/light | `src/styles/*.css` | **keep, adapt** | the owner likes it (D24); see [webapp-style.md](../references/webapp-style.md) |
| Lobby flow: table list, create and join modals, buy-in vs bankroll check | `src/pages/LobbySystem.js`, `src/components/lobby/` | **concept only** | right flow, rebuilt in SwiftUI |
| Action panel: to-call maths, fold/check/call/raise | `src/components/game/ActionPanel.js` | **concept only** | add all-in and configurable blinds |
| Hole-card privacy pattern: hidden until tapped, confirm or retry | `src/components/game/CardScanner.js` | **concept only** | core to PRODUCT.md's privacy principle |
| Frame streaming to socket.io | `CardScanner.js`, `src/App.js` | **rewrite** | iOS camera, possibly on-device Core ML |
| Owner calibration page | `src/pages/CalibrationPage.js` | **concept only** | calibration stays a need |
| STOMP client service | `src/services/GameWebSocketService.js` | **rewrite** | Swift |
| State-based "router", debug page | `src/App.js`, `src/pages/GameDebug.js` | **drop** | |

## Card detection (`cv/`)
| Piece | Path | Verdict | Why |
|---|---|---|---|
| YOLO model | `cv/models/best_60_23.pt` | **keep as-is** | 52 classes, works; accuracy still to be measured (spike) |
| Duplicate-removing reader | `cv/utils.py` `get_n_cards` | **keep as-is** | simple and correct |
| Image decoding | `cv/utils.py` `process_raw_image` | **keep as-is** | |
| Mat calibration (red spades, crop) | `cv/camera.py` | **keep, adapt** | the spike decides whether this mat-based approach holds |
| Community-card reading | `cv/utils.py` `get_comm_cards` | **rewrite** | a stub today |
| Flask-SocketIO transport | `cv/app.py` | **concept only** | transport is decided in the V1 grill |

## Predecessor repos (`lucabzt/Spade@1510db9`)
| Piece | Path there | Verdict | Why |
|---|---|---|---|
| 436 voice-dealer clips | `server/assets/sounds/` | **keep as-is** (later) | ready-made dealer voice; contains friends' names, stays out of the public repo |
| Equity engine (Holdem Calculator, MIT) | `server/src/engine/` | **keep, adapt** (later) | the only working win-probability code |
| 40 hand-analysis tests | `server/src/tests/test_analysis/` | **keep, adapt** | port as more vectors for the rebuilt evaluator |
| Ledger spreadsheet formulas (chip values, zero-sum check) | `Poker_Chip_Tracker.xlsx` | **concept only** | the ledger feature's rules and its import source |
| User stories, UML, target package tree | `zDone`, `UML`, `zUML.txt`, `zFolder.txt` | **concept only** | requirements and design input |
| Flask server, karaoke `Lyrics.py` | `server/` | **drop** | superseded |
