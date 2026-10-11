# Phase 08 · Player app

> **Status:** 🔲 ready, scoped 2026-10-11 · Version 1 · Dealer ([spec](V1_2026-10-11_DEALER.md))
> **Issues:** [#7](https://github.com/eixfachZabii/Spade/issues/7) (closes), [#4](https://github.com/eixfachZabii/Spade/issues/4) (closes)
> **Depends on:** phase 07 · **Rests on:** V1 D6, D7, D11–D13, D18 · ADRs [0008](../adr/0008-a-hand-is-a-fold-over-events-and-card-reads-never-block-it.md)–[0012](../adr/0012-v1-stacks-spring-boot-4-react-hub-swiftui-app.md) · [ios-stack.md](V1_evidence/ios-stack.md)
> **Owner first:** install Xcode 27; join the Apple Developer Program before the TestFlight step; Developer Mode on your iPhone.

## Outcome
An iPhone joins a table, shows its owner's hole cards (scanned on the phone or typed), plays every action from the server's legal actions, and lets the dealer enter the board. It plays a full hand against the testworld, and a TestFlight build runs on the owner's phone.

## Scope
### In
- **`ios/`:** a thin Xcode app target (synchronized folders) plus the local package `ios/Packages/SpadeKit`:
  - `SpadeDomain`, `SpadeAPI`, `SpadeScan`, `SpadeUI`;
  - iOS 26; Swift 6 mode, MainActor default isolation; Observation and `NavigationStack`.
- **Design tokens, for both clients:** `tokens/spade.tokens.json` (DTCG) and a dependency-free `tokens/build.mjs` that writes:
  - the Swift colour catalog and `SpadeTokens.swift`;
  - `hub/src/styles/tokens.css` (used in 09).
  The gate checks the outputs are current. Values come from [DESIGN.md](../../DESIGN.md).
- **Accounts:** register (with the invite code), log in once per device; session token in the Keychain (`WhenUnlockedThisDeviceOnly`); devices list and revoke.
- **Lobby:** the host opens a table (blinds in points); players join with a buy-in, top up, and leave with a chip count.
- **Seated:**
  - the table view (stacks, pot, to-call, whose turn, board, showdown result) from the SSE stream;
  - actions from `legalActions`: fold, check, call, raise (slider + presets), all-in;
  - amounts in points;
  - the screen stays awake while seated.
- **Hole cards:**
  - asked for right after the deal, never blocking;
  - **scan:** AVFoundation → Vision → the Core ML model, then `ScanDecider`:
    - per-card best confidence; top two;
    - stability over ≥3 of the last 5 frames;
    - **crop and scale the region so cards land at ~120 px in the model input** (the proxy spike's main finding; held cards can be too big as well as too small);
  - **typed:** a 4×13 picker, one tap away;
  - **press-and-hold to peek;** hidden whenever the app isn't active.
- **Dealer duties:** deal the next hand, then the board (scan or type) when the server asks; any player can correct a card. The host can do every duty.
- **Chip counts** from the seat.
- **The stream:** reconnect on foreground, network change or 45 s of silence; the first snapshot is the resync.
- **The model:**
  - exported to Core ML (FP16, embedded NMS) in its own pinned environment (`cv/export/`, a separate uv project: coremltools 9 needs numpy ≤ 2.3.5);
  - `.mlpackage` in Git LFS, plus a manifest (source `.pt` SHA-256, versions, arguments) the gate checks.
- **Tests:**
  - Swift Testing: `ScanDecider`, the snapshot reducer, card parsing, **decoding every `api/fixtures/` body**, model-on-stills;
  - XCUITest flows against the backend's testworld: join, act, typed hole cards, dealer board, showdown.
- **The gate and CI:**
  - `scripts/gate.sh --ios` runs `swift test`, the tests on a simulator and the manifest check, and prints "not proven" when Xcode is missing;
  - a GitHub Actions macOS job.
- **TestFlight:** an internal build installed on the owner's iPhone.

### Out (and where it goes)
| What | Where |
|---|---|
| joining by the hub's QR, approving a hub pairing | 09 |
| guests' public TestFlight link | 10 |
| win probability display | 12 |
| the chip helper | 12 |

## Decisions
| # | Decision |
|---|---|
| D1 | **The SSE client:** LDSwiftEventSource if its licence allows, else a ~50-line parser over `URLSession.bytes`. Decide at the start. |
| D2 | **Model precision FP16** (21.4 MB). INT8 only if real photos read the same. |
| D3 | **`ScanDecider` starts with a 0.5 confidence floor** and a 0.7 "sure" line (from the proxy spike), tuned on real photos. |
| D4 | **Peeking is press-and-hold** (the legacy app toggled). |

## Done when
- XCUITest plays a full hand against the testworld.
- All `api/fixtures/` bodies decode in Swift tests.
- `scripts/gate.sh --ios` and the macOS CI job are green.
- **`verify-change` §4: a number.** *N* photos of the owner's real hole cards under the table's light, *X* read right on the device, with time per read. If the cards still aren't at hand, this goes to "not proven" and #3 carries it.
- A TestFlight build runs on the owner's iPhone against a backend on the Mac.

## Risks
- **First Xcode project in the repo:** project-file merge conflicts. Synchronized folders and the package keep the target thin.
- **Whether Swift Vision's `CoreMLRequest` returns detection observations for an NMS pipeline model:** fall back to `VNCoreMLRequest`.

## Not proven by this phase (expected)
- guests' installs (10);
- other friends' older iPhones;
- the stream through the tunnel (10).

## What shipped, against what was scoped
*(filled in at ship)*

## What shipping it actually found
*(filled in at ship)*
