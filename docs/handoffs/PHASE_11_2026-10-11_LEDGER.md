# Phase 11 · Ledger

> **Status:** 🔲 ready, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Issues:** [#11](https://github.com/eixfachZabii/Spade/issues/11) (closes)
> **Depends on:** phases 06–10 · **Rests on:** V1 D3, D4, D5 · ADR [0006](../adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md) · [the spreadsheet](../references/poker-chip-tracker.xlsx)

## Outcome
Every poker night lives in Spade's ledger, the 20 spreadsheet nights included: buy-ins, counted cash-outs, results that sum to zero, and each player's P&L over nights on the hub. The spreadsheet retires.

## Scope
### In
- **A poker night** is a table from open to close. Its buy-ins and cash-outs are the rows recorded since phase 06.
- **Counted cash-out by denomination:** the app lets a leaving player enter their chips per denomination (5, 10, 25, 100 points), as the spreadsheet did, and Spade sums them.
- **The zero-sum check:** a night whose results don't sum to zero is saved with its difference shown, never blocked (the chips are the truth; the count may be wrong).
- **Results and P&L:** result = cash-out − buy-ins per player per night, in euros (100 points = €1); P&L = results summed over nights.
- **The import:**
  - a one-off, idempotent command reads `docs/references/poker-chip-tracker.xlsx`: 20 sheets, one per night (`SPIELER`, `WIN / LOSS`, `Total Bet`, chip counts per denomination);
  - names map to accounts through a mapping file;
  - guests (`GAST #1`, …) become players without a login.
  - The imported nights must reproduce the spreadsheet's `WIN / LOSS` to the cent.
- **Hub:** a ledger page with nights as a table and **P&L over nights as a hand-written SVG line chart**, with labels at the line ends (no hover on a TV) and euros.
- **App:** your own P&L and your nights.

### Out
- money settlement between players (outside Spade);
- per-hand history (#16, later).

## Decisions
| # | Decision |
|---|---|
| D1 | **The import is a command, not a migration:** real names and money stay out of schema history. |
| D2 | **Guests in old nights become login-less players;** a guest who later installs the app can be merged into their account by the host. |

## Done when
- The import reproduces every spreadsheet night's results exactly (a test per night, against values transcribed from the sheet, never from app output).
- A new night recorded through the app shows on the hub.
- The zero-sum difference shows on a deliberately miscounted night.
- The gate is green.

## Not proven by this phase (expected)
- that the group trusts the ledger over the spreadsheet; that takes a few nights.

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
