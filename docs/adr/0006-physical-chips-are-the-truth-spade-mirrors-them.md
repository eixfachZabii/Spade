# 6. Physical chips are the truth; Spade mirrors them

Date: 2026-10-10
Status: Accepted
Relates to: [ADR 0002](0002-physical-cards-camera-reads-manual-betting.md) (which left this open as assumption A1) · [PRODUCT.md](../../PRODUCT.md) · V1 grill

## Context
ADR 0002 put real cards on the table and bets on the phone, but left open whether real chips stay too (A1). An app could make its own stacks the only truth: no chip counting, and cash-outs straight into the ledger. The group plays with chips, and the owner chose to keep them.

## Decision
- **The physical chips decide.** Every bet is pushed in chips *and* tapped on the phone. Spade's stacks and pots are a mirror of the chips, used to:
  - run the hand: whose turn, the amount to call, legal actions, all-in;
  - split side pots;
  - say who wins which pot.
- **When the mirror and the chips disagree, the chips win.** A **chip count** sets a player's stack by hand; it is a correction like any other, and the hand never stops for it.
- **Buy-in and cash-out are counted, not computed:**
  - a buy-in records the money a player turned into chips;
  - a cash-out records the chips the player counted at the end.
  The ledger stores those. Spade's mirror only flags a difference, it never overrules a count.

## Consequences
- The betting UI still matters (Spade can't decide side pots without it), but a mis-tap costs a correction, not a wrong payout.
- The ledger's zero-sum check (per night, the counted cash-outs equal the buy-ins) is the real audit, as it was in the spreadsheet.
- The chip optimiser and the chip colour scale stay relevant: the physical chips' denominations are part of the domain (#14).
- ADR 0002's consequence "physical chips are not the source of truth (assumption A1)" is reversed by this record.
