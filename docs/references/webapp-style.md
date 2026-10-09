# The webapp's style: reference for the new look

> **Origin:** `webapp/src/styles/*.css` (`base.css`, `components.css`, `layout.css`, `pages.css`), last changed in commit `5f20f31` (2025-07-21). Values are copied exactly; nothing here is redesigned.
> **Why it is here:** *"honestly I did kinda like the style and vibe of the webapp that we had we can apply the same style for the frontend"* (owner, 2026-10-09; V0 spec D24). This is **input for `/impeccable` in V1**, not a design system.

## Colour tokens
Dark is the default (`:root`); light overrides it via `.light-theme` (`base.css`).

| Token | Dark | Light | Used for |
|---|---|---|---|
| `--primary-color` | `#6a11cb` | (same) | brand purple; focus, accents |
| `--secondary-color` | `#2575fc` | (same) | brand blue |
| `--primary-gradient` | `linear-gradient(to right, #6a11cb, #2575fc)` | (same) | **the signature**: primary buttons, Raise, highlights |
| `--success-color` | `#10b981` | `#059669` | Call, confirmations |
| `--warning-color` | `#f59e0b` | `#d97706` | warnings |
| `--danger-color` | `#ef4444` | `#dc2626` | Fold, delete |
| `--info-color` | `#3b82f6` | `#2563eb` | info |
| `--text-primary` | `#f3f4f6` | `#111827` | body text |
| `--text-secondary` | `#9ca3af` | `#4b5563` | secondary text |
| `--bg-primary` | `#111827` | `#f9fafb` | page |
| `--bg-secondary` | `#1f2937` | `#f3f4f6` | panels |
| `--bg-tertiary` | `#374151` | `#e5e7eb` | inputs, hover |
| `--card-bg` | `#1e293b` | `#ffffff` | cards (table cards, modals) |
| `--border-color` | `#4b5563` | `#d1d5db` | borders |

The grays are Tailwind's gray scale (`gray-900` … `gray-100`), with `slate-800` (`#1e293b`) for cards.

## Typography
- Body: `"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, …`.
- Monospace (debug page only): `'Courier New', monospace`.

## Shape, depth, motion
- Radius: `--border-radius: 12px` almost everywhere; `8px` for small controls; `50%` for avatars and chips.
- Shadow: `--card-shadow: 0 10px 15px -3px rgba(0,0,0,.1), 0 4px 6px -2px rgba(0,0,0,.05)`; brand-tinted `0 4px 6px rgba(106,17,203,.2)` on primary buttons.
- Motion: `--transition-speed: 0.3s`. Keyframes: `fadeIn` (fade + 10px rise), `cardFlipIn` / `cardFlipOut`, `pulse`, `turnPulse` (whose turn), `scanning` (scanner), `shine`, `spin`.
- Modals use a `backdrop-filter: blur(4px)` scrim.

## Component patterns
| Pattern | Where | What it looks like |
|---|---|---|
| Action buttons | `pages.css` `.action-button.{raise,call,check,fold}` | big, rounded, white text, each its own diagonal gradient: Raise `#6a11cb → #2575fc`, Call `#10b981 → #048c61`, Check `#12161b → #313337`, Fold `#ef4444 → #dc2626`; a light overlay on hover |
| Hole cards, hidden until tapped | `components.css` `.card-back`, `.reveal-hint`, `cardFlipIn/Out` | face-down card with a small "tap to reveal" hint; flips to show rank and suit (red/black suits, `.black-card` adapts in light mode) |
| Card scanner | `components.css` `.card-scanner`, `.cam-container`, `.camera-security-warning` | camera view with a scanning animation, then a verification step (confirm or retry) |
| Table cards in the lobby | `components.css`, `pages.css` | `--card-bg` cards with `--card-shadow`, buy-in limits and player count, a featured banner |
| Stack and pot readouts | `.chips-display`, `.balance-indicator` | label + large value pairs |
| Header | `layout.css` | logo, theme toggle (dark/light), connection indicator |

## What to keep, what to refine
- **Keep the vibe:** dark-first, calm gray surfaces, one bold purple→blue gradient reserved for the important action, colour-coded poker actions, generous 12px radii, small flip and pulse motions.
- **Refine in V1 (with `/impeccable`):** a real type scale; contrast checks for the gradient on both themes; tokens shared by the web hub and the iOS app (SwiftUI colour assets); the logo (the webapp used a generic poker-hand icon instead of the Spade mark).
