# User stories from 2025

> **Origin:** `lucabzt/Spade@1510db9:zDone`, lines 469–476. That file is a raw SiemensGPT chat transcript (Claude 3.7 Sonnet); **only these stories are carried over, in our own words, never its generated code.**
> Status is judged against the play model ([ADR 0002](../adr/0002-physical-cards-camera-reads-manual-betting.md)) and the rebuild ([ADR 0005](../adr/0005-rebuild-dont-repair.md)).

1. **Log in and see my bankroll.** A player signs in with their own account and sees their money. *Kept.*
2. **Create a game.** A player opens a table and invites others to it. *Reframed by ADR 0002:* the "game" is the physical table at the poker night; creating it means opening the night's table in Spade.
3. **Join with part of my bankroll.** A player sits down with a buy-in taken from their bankroll. *Kept.*
4. **Watch as a spectator.** Someone follows the game without playing. *Open, needs a grill:* the hub already shows the table; whether a spectator role adds anything is undecided.
5. **Invite players.** A table owner invites others. *Replaced:* the 2025 code built friends instead; whether either survives is an open issue.
6. **Play standard poker.** Bet, fold, raise and check by the rules. *Kept*, with real cards and typed bets (ADR 0002).
7. **Leave with my chips back.** Leaving returns the remaining stack to the bankroll, so the money stays right. *Kept.* The 2025 engine broke this: results were never saved.
8. **Log every game.** Every hand is recorded for replay and fairness review. *Open, needs a grill:* the ledger needs results per night; hand-by-hand history is a later idea.
