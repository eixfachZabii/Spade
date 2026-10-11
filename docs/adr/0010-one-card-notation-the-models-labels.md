# 10. One card notation: the model's labels (`AS`, `10H`)

Date: 2026-10-10
Status: Accepted
Relates to: [#5](https://github.com/eixfachZabii/Spade/issues/5) · [legacy API, card notation](../references/legacy-api.md#card-notation-today) · V1 grill

## Context
Three notations exist:
- cv's model labels: `AS`, `10H`, `JD`;
- the 2025 backend's `Value.name()` + suit initial: `ACEH`, `JACKH`;
- the legacy hub's `THREES`-style.

The model's 52 labels are fixed by its training; the other two die with the rewrite (ADR 0005). CLAUDE.md forbids adding a fourth.

## Decision
- **Everywhere a card leaves memory, it is written as the model writes it:** on the wire, in the database, in fixtures and in tests.
  - **Rank:** `2`–`10`, `J`, `Q`, `K`, `A`.
  - **Suit:** `C`, `D`, `H`, `S`.
  - Upper case, a JSON string: `"10H"`, `"AS"`.
- In code, a card is a value type (Java record, Swift struct, TS type) parsed from that string at the edge. Every layer rejects an unknown string.
- Rendering (a card image or a drawn card) maps from this notation in exactly one function per client, tested over all 52 cards.

## Considered options
- **`T` for ten** (`TH`), the poker-software convention. It would need a conversion at the model's output and would be the fourth notation.
- **A `{rank, suit}` object.** Clearer to read, heavier on the wire and in fixtures; the value type gives the same safety in code.

## Consequences
- The model's output goes straight into the engine without conversion.
- Porting the 18 evaluator vectors and the predecessor's cases means rewriting them in this notation once.
- The legacy `ACEH` and `THREES` formats survive only in frozen code until it is deleted.
