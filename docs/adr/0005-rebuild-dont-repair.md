# 5. Rebuild, don't repair: a new web hub, a native iOS player app, a simpler backend

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D21–D25 · [salvage map](../status-quo/salvage.md) · [webapp style](../references/webapp-style.md)

## Context
The 2025 code works in places and is broken in many others (see [status-quo](../status-quo/README.md)). The hub `client/` is a Vision UI template with no style of its own. The phone app `webapp/` needs to be on iOS. The owner: *"take the best parts of it, look at wealth watcher for architecture inspiration as well and just code it fully new where things can't be saved."*

## Decision
- **Salvage first.** The [salvage map](../status-quo/salvage.md) names what is kept, adapted, kept only as a concept, rewritten or dropped. Nothing is patched in place for its own sake.
- **Backend:** rebuilt simpler where it can't be saved, borrowing WealthWatcher's layering (thin routes → services → repositories, a pure domain, explicit API schemas).
- **Hub:** a new web dashboard. The old pages are the concept; all of the code and libraries are new.
- **Player app:** native iOS in Swift. It replaces `webapp/`.
- **Look:** the legacy webapp's style and vibe, refined into a design system with the design skills. A new style; not WealthWatcher's.
- **Not decided here:** stacks, libraries and project names. They come from V1's research and grill.

## Consequences
- `client/` and `webapp/` stay frozen until their replacements ship, then they are deleted.
- iOS needs Xcode and macOS CI runners, which are slower and pricier than Linux. The gate grows an iOS block.
- On-device hole-card detection (the YOLO model exported to Core ML) becomes possible: hole-card images would never leave the phone.
- The V1 grill needs five pieces of evidence before it can decide anything (see [INDEX](../handoffs/INDEX.md)).
