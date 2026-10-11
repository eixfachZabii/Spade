# Phase 07 · The hand

> **Status:** 🔲 ready, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Issues:** [#1](https://github.com/eixfachZabii/Spade/issues/1) (closes), [#2](https://github.com/eixfachZabii/Spade/issues/2) (closes)
> **Depends on:** phase 06 · **Rests on:** V1 D3, D6–D9 · ADRs [0006](../adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md), [0008](../adr/0008-a-hand-is-a-fold-over-events-and-card-reads-never-block-it.md), [0009](../adr/0009-commands-over-rest-per-viewer-snapshots-over-sse.md)

## Outcome
A whole hand runs on the server from events: blinds, four streets, all-ins and side pots, a showdown that names who wins each pot. Card reads and corrections arrive late without blocking anything. Every client receives its own table view over SSE, and no view ever holds another seat's hole cards before showdown.

## Scope
### In
- **`domain.hand`** (pure, ADR 0008):
  - sealed `HandEvent`s:
    - hand started (dealer, seats, blinds);
    - hole cards read;
    - board read;
    - card corrected;
    - player acted;
    - stack counted;
    - hand voided.
    Each card event has a source: phone scan, typed, table camera.
  - `HandState.apply` returns the next state or throws an illegal-move exception.
  - Streets advance from the betting alone.
  - Showdown: when the cards are complete, `Showdown.winners` per pot.
- **Rules:**
  - blinds posted automatically;
  - the button moves to the next occupied seat;
  - heads-up: the button posts the small blind and acts first pre-flop;
  - fold, check, call, bet/raise, all-in;
  - **the minimum raise is the last full raise**, never just the big blind (legacy defect);
  - a short all-in does not reopen the action;
  - side pots from every all-in level; an odd chip goes to the first winner left of the button.
- **The server computes for each seat:** legal actions, to-call, min/max raise.
- **Card reads:**
  - a card already on the board or claimed by another seat is **rejected with a clear problem**;
  - a correction replaces a read.
  - Hole cards are asked for right after the deal and never block.
  - At showdown or an all-in, Spade names the seats whose cards it still needs.
- **Chips:**
  - a **chip count** sets a stack (the chips are the truth);
  - a hand can be **voided** (a misdeal): its bets are returned in the mirror.
  - **A hand closes when the next one is dealt.**
- **Persistence.** One `HandService.submit(tableId, event, principal)`:
  - a per-table lock;
  - append to `hand_events`;
  - update the projection and save results in the same transaction;
  - publish after commit.
- **Commands (REST, idempotent by client id):**
  - deal next hand (the dealer, or the host when the dealer's phone is gone);
  - act;
  - hole cards;
  - board cards;
  - correct a card;
  - count a stack;
  - void the hand.
- **Table views and the stream** (ADR 0009):
  - `PublicTableView` has no hole-card field; it carries the revealed hands at showdown.
    - The private hole-card type is named `HoleCards`. The ArchUnit rule `publicViewsNeverNameHoleCards` (Phase 06) forbids it in any `Public*View` / `*PublicView`, nested types included; this phase re-plants the rule against its real types.
  - `PlayerTableView` = the public view + your own seat (cards, to-call, legal actions).
  - `GET /api/v1/tables/{id}/view` and `GET /api/v1/tables/{id}/stream` (SSE):
    - a full snapshot on connect and after every commit;
    - a monotonic `version`;
    - a heartbeat every 20 s.
- **Privacy, structurally:**
  - an ArchUnit rule plus a **property test**: random scripted hands, every view for every viewer, no foreign hole card before showdown;
  - `verify-change` §3 with two real sessions (`curl` on both streams).
- **The testworld** gains:
  - a scripted card source (hands from versioned JSON built from the evaluator vectors);
  - a scripted-hand driver for the clients' e2e tests.
- **Contract:** the snapshots of one scripted hand (every view a player and the public receive) go to `api/fixtures/`.
- **Golden masters:** side-pot and split cases worked out by hand, plus the winner suite.

### Out (and where it goes)
| What | Where |
|---|---|
| win probability | 12 |
| clients | 08, 09 |
| the table camera as a source | 13 |
| per-hand history screens | later, #16 |

## Decisions
| # | Decision |
|---|---|
| D1 | **Every hand that reaches showdown is revealed in the public view;** they are face up on the table anyway. Folded hands are never shown, to anyone, ever. |
| D2 | **No shot clock.** Thinking time is shown, never enforced. |
| D3 | **The host can do any dealer duty** (deal, enter the board, void), so a dead phone never stops the night. |
| D4 | SSE heartbeat 20 s (under Cloudflare's ~100 s idle limit); clients reconnect on 45 s of silence. |
| D5 | A correction after the hand closes is refused; the chips are the truth, and a chip count fixes a stack. |

## Done when
- The engine's unit tests cover:
  - heads-up;
  - three-way all-ins with two side pots;
  - a short all-in that doesn't reopen;
  - an odd chip;
  - split pots;
  - the winner suite.
- The testworld plays a scripted 6-seat night of several hands end to end, through REST and SSE.
- The privacy property test passes, and the `verify-change` privacy check passes with two real sessions.
- Every legacy engine defect listed in #1 is checked off with the test that guards it.
- `scripts/gate.sh` is green; the contract fixtures are committed.

## Risks
- **Spring Security on `SseEmitter`'s async response:** a known trap; test it first.
- **Corrections that change the betting's context** (e.g. a corrected hole card at an all-in) need clear rules; keep them to cards, never amounts.

## Not proven by this phase (expected)
- no human has played through a client (08, 09);
- the stream through Cloudflare (10).

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
