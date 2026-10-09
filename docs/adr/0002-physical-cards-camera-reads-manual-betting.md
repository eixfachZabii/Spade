# 2. Real cards are dealt by a person; cameras read them; bets are typed on the phone

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/done/Version0.0/V0_2026-10-09_FOUNDATION.md) D1–D3 · [PRODUCT.md](../../PRODUCT.md)

## Context
Spade exists for our own poker nights (D1). The 2025 code assumed two different products at once: the README promised a camera-driven dealer for a physical table, while the engine shuffled and dealt its own virtual deck. The owner settled it: *"phone camera recognizes your own player cards. table cam reads community cards. chips like raise are entered manually. I want the card detection."*

## Decision
- A person shuffles and deals physical cards. Spade never deals.
- Each player's phone camera reads that player's hole cards. The table camera reads the board.
- Bets, calls, raises and folds are entered by hand on the phone.
- Spade runs the hand, holds the pot and the stacks, decides the showdown, announces the result and records it.
- Card detection is core scope.

## Consequences
- The engine becomes a state machine that takes in card reads, instead of a dealer (the V1 backend-rebuild issue).
- Every camera read needs a manual correction path, because the night must not stop for Spade (PRODUCT.md).
- Community-card detection was never built, which makes it V1's biggest risk; a spike measures it first.
- Physical chips are not the source of truth (assumption A1, to confirm in the V1 grill).
