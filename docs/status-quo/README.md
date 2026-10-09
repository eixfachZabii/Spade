# Status quo, 2026-10-09

How Spade looked when V0 started: the "before" picture the rebuild works from. These files are dated snapshots and are **not maintained**; each has a "Changed since" box for the few statements V0 itself made untrue.

| File | Covers |
|---|---|
| [backend.md](backend.md) | `spadeboot/`: stack, packages, domain model, API, feature status, engine defects, security |
| [frontends.md](frontends.md) | `client/` (hub) and `webapp/` (phone): routes, flows, contract mismatches, legacy bugs |
| [cv.md](cv.md) | `cv/`: what card detection does and doesn't |
| [lineage.md](lineage.md) | the predecessor repos and what was lost |
| [salvage.md](salvage.md) | **what V1 takes from all of it** |

## At a glance
| Area | Works | Partial | Dead or missing |
|---|---|---|---|
| Accounts | register, login, profile, avatar, admin | username change logs you out | |
| Lobby | create, join, leave, buy-in | table deletable mid-game | |
| Playing a hand | blinds, streets, actions, hand ranking (fixed in V0) | no side pots, results not saved, polling instead of events | showdown and winner never shown |
| Cards | the YOLO model reads hole cards from photos | | phone scanning never reaches cv; board reading is a stub; nothing feeds the engine |
| Hub extras | cheatsheet heatmap, Spotify | chip optimiser (paid solver), analytics (hard-coded) | |
| History and stats | | | replays, statistics, invitations commented out |

## Top risks for V1
1. **Board reading does not exist.** The play model ([ADR 0002](../adr/0002-physical-cards-camera-reads-manual-betting.md)) depends on it; the spike comes first.
2. **Privacy was never enforced.** Every hole card reached every client. The rebuild must make "private until showdown" structural.
3. **The engine has no persistence.** Money moved at a table was never saved.
4. **Three card notations** across cv, backend and frontend.
5. **No working deployment.** Docker is broken and nothing is hosted; where Spade runs on a poker night is undecided.
