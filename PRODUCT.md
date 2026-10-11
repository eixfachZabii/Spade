# Spade: product

## One sentence
Spade is the dealer's brain for **our own poker nights**: real cards on a real table, while Spade reads them, runs the hand, calls the winner and keeps the books.

## Who it is for
One friend group, physically at one table, playing Texas Hold'em together. **Everyone in the group has an iPhone** (owner, 2026-10-09), so the player app is iPhone-only and needs no fallback. Success is simple: **we use it every poker night** ([ADR 0002](docs/adr/0002-physical-cards-camera-reads-manual-betting.md)). That is not a product for strangers, and not a demo.

## How a night works
1. Everyone joins the table in the Spade iPhone app, with a buy-in from their bankroll.
2. A person shuffles and deals real cards.
3. Each player scans their two hole cards with the app. Only they see them.
4. The table camera reads the flop, turn and river.
5. Players push chips and tap fold / check / call / raise; Spade mirrors the pot and every stack.
6. At showdown Spade knows every hand: it decides the winner (side pots included), announces who gets which pot, and records the result. The chips are pushed by hand.
7. The big screen shows the table: board, pot, whose turn it is, the announcements.
8. At the end, everyone counts their chips; the cash-outs land in the ledger. No more spreadsheet.

## Principles
Confirmed by the owner in the V1 grill, 2026-10-11 (they were proposals until then, V0 spec A2). Every V1 phase tests against them.
- **The night never stops for Spade.** Every camera read can be corrected by hand in seconds. A misread is an inconvenience, never a dead end.
- **Hole cards stay private until showdown.** No screen and no API response shows another player's cards earlier.
- **Reliability beats magic.** A plain feature that always works beats a clever one that works most nights.
- **Setup in minutes.** From "cards are out" to the first hand in under five minutes, with no laptop fiddling.
- **The iPhone app is the only device per player** ([ADR 0005](docs/adr/0005-rebuild-dont-repair.md)). Install it once, then no account juggling at the table. Guests install it too.

## What Spade is not
- Not online poker: everyone is at the same table.
- Not a real-money gambling platform: stacks are the group's own bookkeeping.
- Not a public product: one group, our nights.

## Open questions (V1 grill)
- ~~Are physical chips still on the table, or are Spade's stacks the only truth? (A1)~~ **Settled 2026-10-10: the chips are the truth; Spade mirrors them** ([ADR 0006](docs/adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md)).
- ~~Where does Spade run: a laptop at the table, or `hub.poker-spade.de`? (A3)~~ **Settled 2026-10-10: on the owner's Linux PC at the table, reached through a Cloudflare Tunnel on our own domain** ([ADR 0007](docs/adr/0007-spade-runs-on-the-owners-linux-pc-behind-a-cloudflare-tunnel.md)).
- ~~Which extras earn their place?~~ **Settled 2026-10-10:** V1 has the ledger (with the 20 old nights and P&L over nights), the cheatsheet and chip helper, win probability at all-in, and Spotify's now-playing. The voice dealer, per-hand history and friends wait ([V1 spec](docs/handoffs/V1_2026-10-11_DEALER.md)).
