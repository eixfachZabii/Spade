# Phase 12 · Night extras

> **Status:** 🔲 ready, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Issues:** [#14](https://github.com/eixfachZabii/Spade/issues/14), [#12](https://github.com/eixfachZabii/Spade/issues/12), [#13](https://github.com/eixfachZabii/Spade/issues/13) (all close)
> **Depends on:** phases 07–09 · **Rests on:** V1 D5, D17 · [salvage map](../status-quo/salvage.md) (heatmap data, chip optimiser, chip colours)

## Outcome
The hub earns its place between hands:
- a cheatsheet;
- a chip helper for buy-ins;
- TV-style win probability once all-in hands are known;
- what's playing on Spotify.

## Scope
### In
- **Cheatsheet (#14):** the 169-hand heatmap, its data taken as-is from `CheatsheetService`, as a hub page.
- **Chip helper (#14):**
  - given a buy-in in points and the chips available per denomination, propose a distribution;
  - a small exact solver: no Gurobi, no network call;
  - in the app (at buy-in) and on the hub;
  - the chip colour scale from the legacy hub.
- **Win probability (#12):**
  - shown on the hub next to each remaining seat **only when every remaining player's hole cards are known and no more betting can happen** (CONTEXT: win probability);
  - computed in Java with the ported evaluator by exact enumeration of the remaining board;
  - the cost is measured first, with Monte Carlo as the fallback if pre-flop multiway is too slow;
  - it appears only in the public view once the cards are face up, so privacy holds by construction.
- **Spotify now-playing (#13):**
  - one linked account (the host's) via OAuth authorization code with PKCE and a checked `state`;
  - the refresh token encrypted at rest with a key from `.env`, never sent to a client;
  - the backend polls the current track;
  - a corner widget on the hub with cover, title and artist;
  - no playback and no lyrics (V1 D17).

### Out
- the voice dealer (#10, later);
- per-hand history (#16, later).

## Decisions
| # | Decision |
|---|---|
| D1 | **Win probability never appears while betting is still possible,** even if every hole card is known (it would coach the action). |
| D2 | **Spotify tokens stay on the server;** the hub receives only track metadata (#2's Spotify holes, closed by design). |

## Done when
- Win probability matches a known reference for the all-in scenarios of phase 07's golden masters, with a measured time per computation.
- The chip helper's distributions sum exactly to the buy-in for every denomination set tested.
- The Spotify widget survives token refresh over a night.
- The gate is green.

## Not proven by this phase (expected)
- Spotify's rate limits over many nights.

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
