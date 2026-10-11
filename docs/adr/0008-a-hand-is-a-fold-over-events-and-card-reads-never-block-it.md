# 8. A hand is a fold over events, and card reads never block it

Date: 2026-10-10
Status: Accepted
Relates to: [ADR 0002](0002-physical-cards-camera-reads-manual-betting.md) · [ADR 0006](0006-physical-chips-are-the-truth-spade-mirrors-them.md) · [#1](https://github.com/eixfachZabii/Spade/issues/1) · [#3](https://github.com/eixfachZabii/Spade/issues/3) · [wealthwatcher-architecture.md (a)](../handoffs/V1_evidence/wealthwatcher-architecture.md#a-a-slimmed-spring-boot-backend) · V1 grill

## Context
The 2025 engine dealt its own deck on threads, never saved a result and died silently on an exception ([backend status quo](../status-quo/backend.md)). Physical cards and chips (ADRs 0002, 0006) mean Spade learns cards late, from a phone or a camera, and sometimes wrong. Reading the board by camera is unproven ([proxy spike](../handoffs/V1_evidence/cv-proxy-spike.md); #3 is open), and the owner ruled that board reading must not block a night.

## Decision
- **A hand is a pure fold over its events.** `HandState.apply(event)` returns the next state or rejects an illegal move; it has no framework, threads or I/O.
- **The events:**
  - a hand starts (dealer, seats, blinds);
  - hole cards are read;
  - board cards are read;
  - a card is corrected;
  - a player acts;
  - a stack is counted;
  - a hand is abandoned.
  Every card event carries its **source**: a phone scan, a typed entry, or the table camera.
- **Betting never waits for cards.**
  - Streets advance from the betting alone; the dealer deals physically whatever Spade knows.
  - Spade needs card identities only to decide a showdown, and at an all-in to show win probability.
  - When a decision needs a missing card, Spade asks for it then.
- **The dealer's phone types or scans the board** until a table camera exists; any seated player may correct a card. The table camera becomes one more source when the real spike (#3) passes, never a requirement.
- **One way in:** every event enters through one service per table, serialised per table. It is appended to the hand's event log and projected, in one transaction, and published to clients after commit.
- **The server computes everything a client needs to act:** legal actions, to-call, min-raise, all-in, and side pots with odd chips. Clients never re-implement poker rules.
- **A hand closes when the next one starts.** Cards in a closed hand are not corrected. The chips are the truth, so a wrong payout is fixed by a chip count.

## Consequences
- The 18 evaluator vectors and the predecessor's hand-analysis cases become the engine's spec; side-pot cases are added by hand.
- A misread is caught early when hole cards arrive before showdown: a card already on the board or claimed by another seat is rejected or flagged.
- Replaying a hand's events rebuilds it, which is also how tests and the testworld drive the engine.
- The UI shows a street's board as "waiting for cards" without stopping the betting.
