# Phase 13 · Table camera (gated)

> **Status:** 🔲 **gated on [#3](https://github.com/eixfachZabii/Spade/issues/3)**, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Gate:** this phase starts only if the real spike (#3) passes the bar below; otherwise it moves to Version 2, and V1 ships without it.
> **Rests on:** V1 D6 · ADR [0008](../adr/0008-a-hand-is-a-fold-over-events-and-card-reads-never-block-it.md) · [the proxy spike](V1_evidence/cv-proxy-spike.md)

## The gate
The real spike, with our deck, a mat with slots, an overhead camera at its real height and poker-night light, must show:
- **≥ 95% of boards read exactly** when the top confidence per slot is ≥ 0.7;
- **every wrong read below that line,** so it is flagged and not trusted;
- **a read within a second** of the cards lying still.

If it falls short, the dealer's typed and scanned board (phase 08) stays the only path, and the camera waits.

## Outcome
A camera over the table reads the flop, turn and river as one more card source. The dealer's phone confirms any read under the confidence line, and the night never waits for it.

## Scope (if the gate passes)
- **The cv service rewritten for the board:**
  - mat calibration (`cv/camera.py`, adapted);
  - per-slot crops scaled so each card is ~120 px in the model input;
  - one answer per slot (top confidence);
  - stability over frames;
  - the confidence line.
- **A source, not a socket:**
  - the service posts board reads to the backend's REST API as source `TABLE_CAMERA`, with its own revocable device session (ADR 0011);
  - Flask-SocketIO is removed.
- It runs **on the PC**, in the compose stack, on the GPU.
- **The dealer's phone:** a read under the line arrives as "check this card", with one-tap confirm or correct.
- **Accuracy is measured on real night photos** (`verify-change` §4: a number).

## Done when
- One real night with the camera on: reads, flagged reads, corrections and time per read in this doc.
- The gate is green.

## Not proven (expected)
- other tables, lights or decks.

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
