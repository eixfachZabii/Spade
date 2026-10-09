# Spade: product

## One sentence
Spade is the dealer's brain for **our own poker nights**: real cards on a real table, while Spade reads them, runs the hand, calls the winner and keeps the books.

## Who it is for
One friend group, physically at one table, playing Texas Hold'em together. Success is simple: **we use it every poker night** ([ADR 0002](docs/adr/0002-physical-cards-camera-reads-manual-betting.md)). That is not a product for strangers, and not a demo.

## How a night works
1. Everyone joins the table in the Spade iPhone app, with a buy-in from their bankroll.
2. A person shuffles and deals real cards.
3. Each player scans their two hole cards with the app. Only they see them.
4. The table camera reads the flop, turn and river.
5. Players tap fold / check / call / raise; Spade tracks the pot and every stack.
6. At showdown Spade knows every hand: it decides the winner (side pots included), announces it, pays out and records the result.
7. The big screen shows the table: board, pot, whose turn it is, the announcements.
8. At the end, cash-outs land in the ledger. No more spreadsheet.

## Principles
These are proposals until the V1 grill confirms them (V0 spec A2).
- **The night never stops for Spade.** Every camera read can be corrected by hand in seconds. A misread is an inconvenience, never a dead end.
- **Hole cards stay private until showdown.** No screen and no API response shows another player's cards earlier.
- **Reliability beats magic.** A plain feature that always works beats a clever one that works most nights.
- **Setup in minutes.** From "cards are out" to the first hand in under five minutes, with no laptop fiddling.
- **The iPhone app is the only device per player** ([ADR 0005](docs/adr/0005-rebuild-dont-repair.md)). Install it once, then no account juggling at the table.

## What Spade is not
- Not online poker: everyone is at the same table.
- Not a real-money gambling platform: stacks are the group's own bookkeeping.
- Not a public product: one group, our nights.

## Open questions (V1 grill)
- Are physical chips still on the table, or are Spade's stacks the only truth? (A1)
- Where does Spade run: a laptop at the table, or `hub.poker-spade.de`? (A3)
- Which extras earn their place: voice dealer, ledger history, Spotify, cheatsheet, win probability?
- Does everyone at the table have an iPhone? If not, what does a non-iPhone player use?
