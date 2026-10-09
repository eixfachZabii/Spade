---
name: Spade
description: The dealer's brain for our own poker nights. A draft proposal, 2026-10-09, not adopted until the V1 grill picks a direction.
colors:
  brand-purple: "#6a11cb"
  brand-blue: "#2575fc"
  action-call: "#10b981"
  action-call-deep: "#048c61"
  action-fold: "#ef4444"
  action-fold-deep: "#dc2626"
  action-allin: "#f59e0b"
  room: "#111827"
  panel: "#1f2937"
  raised: "#374151"
  slate-card: "#1e293b"
  hairline: "#4b5563"
  ink: "#f3f4f6"
  ink-quiet: "#9ca3af"
  ink-faint: "#6b7280"
  card-face: "#f9fafb"
  card-red: "#dc2626"
  card-black: "#111827"
  flap-red: "#f87171"
typography:
  display:
    fontFamily: "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
    fontSize: "6rem"
    fontWeight: 800
    lineHeight: 0.95
    letterSpacing: "-0.035em"
    fontFeature: "'tnum' 1"
  headline:
    fontFamily: "Inter, -apple-system, sans-serif"
    fontSize: "4rem"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.035em"
    fontFeature: "'tnum' 1"
  title:
    fontFamily: "Inter, -apple-system, sans-serif"
    fontSize: "2.75rem"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "-0.02em"
  body:
    fontFamily: "Inter, -apple-system, sans-serif"
    fontSize: "2rem"
    fontWeight: 500
    lineHeight: 1.3
  label:
    fontFamily: "Inter, -apple-system, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 600
    lineHeight: 1.2
  caption:
    fontFamily: "Inter, -apple-system, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 500
    lineHeight: 1.3
rounded:
  control: "8px"
  card: "12px"
  tv-panel: "24px"
  pill: "999px"
spacing:
  tight: "0.6rem"
  group: "1.1rem"
  section: "2.5rem"
components:
  seat-panel:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "1.1rem 1.4rem"
  turn-marker:
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    padding: "0.25rem 0.8rem"
  playing-card:
    backgroundColor: "{colors.card-face}"
    textColor: "{colors.card-black}"
    rounded: "0.55em"
  stack-figure:
    textColor: "{colors.action-call}"
    typography: "{typography.body}"
---

# Design System: Spade (draft proposal)

> **Status: a proposal for the V1 grill, 2026-10-09.** The tokens are the legacy webapp's, copied exactly from [webapp-style.md](../../../references/webapp-style.md). The TV type scale, the card design, the motion rules and the three directions are new. Nothing here binds until the owner picks a direction; then this file moves to the repo root and loses the sections of the directions not chosen.
> The mockups: [A · The Oval](a-oval.html), [B · Spotlight](b-spotlight.html), [C · Departures](c-departures.html). Comparison and risks: [web-hub-stack.md §10](../web-hub-stack.md#10-design-direction-three-table-views-for-the-owner-to-choose-from).

## Overview
**Creative North Star: "The Dealer's Table at Night."**

Spade is used in a living room or kitchen in the evening. The lamps are dimmed, a TV stands two to four metres from the players, and everyone holds an iPhone in one hand and cards in the other.
- **Dark is right for that scene, not by habit:** a white screen would glare in a dim room and wash out the real cards on the table.
- **The interface is quiet gray, so the real table stays the main event.** One bold purple-to-blue gradient marks the single thing everyone must notice: whose turn it is, and who won.
- **Poker actions keep their colours:** green call, red fold, amber all-in.
- **Motion is rare and means something:** a card flips when the board grows, a ring breathes on the player to act.

**What it must not look like:**
- the 2025 hub's Vision UI dashboard template (navy glassmorphism, `#0075ff`);
- a casino (felt green, gold, chips everywhere);
- WealthWatcher, per [ADR 0005](../../../adr/0005-rebuild-dont-repair.md).

## Colors
**Strategy:** Restrained (neutrals plus one accent) on the iPhone and on the hub's secondary pages. On the hub's table view, the accent may own **one** region at a time (Direction B drenches the actor's panel in it).

### Primary
- **Night Violet → Signal Blue** (`#6a11cb → #2575fc`, the webapp's `--primary-gradient`): the one gradient.
  - Hub: the player to act and the winner.
  - iPhone app: the Raise button and the player's own "your turn" state.
  - Never decoration, never text.

### Secondary (poker actions; unchanged from the webapp)
- **Call Green** (`#10b981`, deep `#048c61`): call buttons; every stack figure.
- **Fold Red** (`#ef4444`, deep `#dc2626`): fold; destructive actions.
- **All-in Amber** (`#f59e0b`): all-in seats, side pots, anything at risk.

### Neutral (Tailwind gray scale, as in the webapp)
- **Room** (`#111827`): the page.
- **Panel** (`#1f2937`): seats, tiles, sheets.
- **Raised** (`#374151`): hover, inputs, quiet borders.
- **Slate Card** (`#1e293b`): the table oval (A) and modal sheets.
- **Hairline** (`#4b5563`): borders.
- **Ink** (`#f3f4f6`), **Ink Quiet** (`#9ca3af`, 6.9:1 on Room), **Ink Faint** (`#6b7280`, 3.9:1 on Room: large text and disabled only).

### Playing cards
- Card faces are physical-card colours, not theme colours: **Card Face** `#f9fafb`, **Card Red** `#dc2626`, **Card Black** `#111827`.
- On dark flap tiles (Direction C) red suits lighten to **Flap Red** `#f87171`.

### Named Rules
**The One Gradient Rule.** At most one gradient-filled element is visible per screen state. If two things want it, one of them is wrong.

**The Gradient Is Not Ink Rule.** Never gradient text.
- White text sits on the gradient: ≥4.5:1 on the purple end, about 4.2:1 on the blue end. So text on the gradient is **large or bold** (≥ 24px bold or ≥ 32px) everywhere.
- The webapp's light theme still needs its contrast checked: a V1 task, not done here.

## Typography
**Body and UI, hub and web:** Inter (self-hosted). It is the webapp's face, and it reads as close kin to SF Pro on the iPhone.
**iPhone app:** SF Pro (system). Shared tokens carry sizes and weights, not the family.
**Direction-specific display faces** (only if that direction is chosen):
- B: **Archivo**, widened (`font-stretch` 112–118%) for names and figures;
- C: **Barlow Condensed** in caps, in flap cells.

**Character:** a plain, sturdy grotesk with tabular figures. Every amount, stack and pot aligns digit for digit, because the numbers are the product.

### Hierarchy (hub TV scale: a 1080p canvas read from 2.5–4 m; 1rem = 16px at 1920×1080)
- **Display** (800, 6rem / 96px, 0.95): the actor's name or the winner (B goes to 10.5rem).
- **Headline** (800, 4rem / 64px): pot and winning amounts.
- **Title** (700, 2.75rem / 44px): seat names, street names.
- **Body** (500–700, 2rem / 32px): stacks, the action line.
- **Label** (600, 1.75rem / 28px): statuses, blinds.
- **Caption** (500, 1.5rem / 24px): the floor; side-pot splits, the footer. Nothing on the hub is smaller.

The iPhone app uses Dynamic Type text styles, not this scale.

### Named Rules
**The 24-Pixel Floor Rule.** Nothing on the hub renders below 24px at 1080p. If it doesn't fit at 24px, it doesn't belong on the TV.

**The Tabular Rule.** Every number uses tabular figures (`font-feature-settings: "tnum"`).

## Layout
- **Hub:** a fixed 16:9 stage, 120 × 67.5rem. The root font size scales it to any 16:9 screen (`font-size: min(100vw/120, 100vh/67.5)`), so 1080p and 4K show the same composition.
  - A: seats on an ellipse (the salvaged `positionUtils` maths);
  - B: a 60/35 split over a six-tile rail;
  - C: a ruled grid with fixed columns that never move.
- **Output 1080p** on weak boxes and let the TV upscale.
- **iPhone:** standard SwiftUI layout; one primary action per screen; the action panel pinned to the bottom within thumb reach.
- **Spacing rhythm:** 0.6rem within a group, 1.1rem between items, 2.5rem between regions; more space above a heading than below it.

## Elevation & Depth
**Tonal layering first, shadows second.**
- Surfaces step Room → Panel → Raised.
- Shadows are neutral, offset and soft (`0 1rem 2rem -0.8rem rgba(0,0,0,.6)`), and only lift things that sit *on* the table: seat panels, cards.
- The brand tint appears in a shadow only under the gradient element itself.

### Shadow vocabulary
- **Seat lift** (`0 1rem 2rem -0.8rem rgba(0,0,0,.6)`): seat panels and tiles.
- **Card lift** (`0 0.35em 0.9em -0.2em rgba(0,0,0,.55), 0 0.08em 0.15em rgba(0,0,0,.35)`): playing cards.
- **Gradient lift** (`0 1.2rem 2.8rem -1rem rgba(37,117,252,.65)`): only under the one gradient element.

### Named Rules
**The No-Glow Rule.** No zero-offset coloured halos. "Whose turn" is shown by the gradient ring and a breathing pseudo-element, not a glowing shadow.

## Shapes
- Radii scale with the surface:
  - controls 8px;
  - cards and seat panels 12px (the webapp's `--border-radius`);
  - large TV panels 24px;
  - status markers are pills;
  - avatars and the dealer button are circles.
- Playing cards use their own radius (0.55em of a card width of 5em), like a real card.
- No borders thicker than 1px, except the 3–4px gradient ring on the active seat.

## Components

### Playing card (signature)
*A real card, made legible from across the room.*
- 5:7 proportions, near-white face.
- **One** top-left index (rank 1.85em, weight 800, suit beneath) and one large pip at the lower right.
- No rotated second index: on a screen a rotated 9 reads as a 6 (found in the mockups).
- **Back:** the brand gradient with a fine diagonal line pattern. Used only for cards Spade knows exist but must not show.
- **Empty board slot:** a dashed outline.
- **States:** `fresh` (flip in, 520ms ease-out), `lift` (part of the winning five), `dim` (not part of it).

### Seat (A and B)
- A Panel container (12px; 24px on B's rail): avatar circle, name (Title), stack (Body, Call Green), status (Label).
- **The player to act:** the gradient ring plus a pill marker ("To act · call €1.20").
- **Folded:** 45% opacity.
- **All-in:** Amber status.
- **Hole cards:** two small backs while live; face-up only at showdown, sent by the server at showdown, never before.

### Board and pot
- The board is 3–5 cards in one row with an empty slot for the next street.
- The pot is "Pot" (Title, Ink Quiet) + amount (Headline); side pots as a Caption line in Amber.
- The board's source is always visible as a footer line (camera, or typed by hand) together with how to correct it.

### Announcement (showdown)
A gradient-filled panel: "Elif wins €15.00" (Headline, white), the hand in words ("Full house, nines full of kings"), then the pot split. It stays until the next hand starts.

### Action buttons (iPhone app; unchanged from the webapp)
- Big, rounded (12px), white text, each with its own diagonal gradient:
  - Raise `#6a11cb → #2575fc`;
  - Call `#10b981 → #048c61`;
  - Check `#12161b → #313337`;
  - Fold `#ef4444 → #dc2626`.
- The buttons come from the server's legal actions; all-in is added.

### Lamp (C only)
A 1.1rem dot before a status word: white = to act, Amber = all-in, Call Green = next, Raised gray = idle or folded.

## Motion
*(Outside the format's eight sections, kept here because motion is part of the identity.)*
- **Durations:** 300ms default (the webapp's `--transition-speed`), 520ms for a card flip; easing `cubic-bezier(0.16, 1, 0.3, 1)`.
- **The board flip:** animate a full `transform` (rotateY) on the card. Decode the face image first, if images are used.
- **The turn pulse:** a breathing ring on a **pseudo-element, animating `transform` and `opacity` only**. It runs for hours, so never animate `box-shadow` (the mockups still do: open item).
- **Flap cascade (C):** a value changes through 4–10 random glyphs in about 70ms steps; never more than one row cascading at a time.
- **Reduced motion:** honour the OS setting *and* a hub setting (TV boxes rarely expose the OS one). Content is visible by default; motion never hides primary content.

## Do's and Don'ts
- **Do** keep one gradient element per screen state.
- **Do** use tabular figures for every amount.
- **Do** show where the board came from and how to fix it, on every table view.
- **Don't** render hole cards on the hub before showdown, and **don't** let the hub receive them: privacy is a server projection, not a CSS rule.
- **Don't** use gradient text, coloured glows, or borders thicker than 1px outside the turn ring.
- **Don't** go below 24px on the hub.
- **Don't** use the PNG deck's rotated-index faces on the hub; draw cards with one index.
- **Don't** bring back the Vision UI look, felt green or casino gold.
