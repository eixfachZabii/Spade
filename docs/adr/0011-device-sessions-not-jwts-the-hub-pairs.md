# 11. Devices hold revocable session tokens, not JWTs; the hub pairs from a phone

Date: 2026-10-10
Status: Accepted
Relates to: [#2](https://github.com/eixfachZabii/Spade/issues/2) · [web-hub-stack.md §7](../handoffs/V1_evidence/web-hub-stack.md#7-how-the-shared-screen-logs-in) · [ios-stack.md §5](../handoffs/V1_evidence/ios-stack.md#5-auth-token-storage) · PRODUCT.md ("install once") · V1 grill

## Context
- **The JWT expires too soon.** The 2025 backend issued a JWT that expired after 24 hours, with no refresh, so everyone would log in again every poker night (PRODUCT.md: "install it once, then no account juggling").
- **The hub logged in as a seated player.** It therefore saw that player's private view, including their hole cards.
- **A JWT can't be revoked** before it expires, which matters for a lost phone or an old hub.

## Decision
- **A player logs in once per device** with a username and password (BCrypt, as today). The server issues an **opaque, random 256-bit session token** for that device. Only its SHA-256 is stored, with the device name and last use.
- **The token is checked on every request**, so revoking it in the app takes effect at once. It doesn't expire on a schedule; it ends on logout or revocation.
- **The iPhone keeps the token in the Keychain** (`WhenUnlockedThisDeviceOnly`).
- **The hub pairs:**
  1. The TV shows a short code and a QR.
  2. The host approves it in the player app.
  3. The hub receives its own session with the `HUB` role: read-only, the public projection only, never a command.
  4. The hub stores it in an HttpOnly, Secure, SameSite=Strict cookie on the same origin.
- **The backend's role vocabulary is `PLAYER`, `HOST` (per table) and `HUB`.**

## Considered options
- **A short JWT plus a rotating refresh token.** Standard, but two token kinds, and revocation still waits for expiry.
- **The host's own login on the hub.** Leaks the host's hole cards to the TV.

## Consequences
- The salvage map's "auth: keep, adapt" keeps the account model, BCrypt and `/me`. The JWT code is dropped.
- A stolen token works until it is revoked. Acceptable for one friend group; it is visible in the device list.
- The secret the backend needs is no longer a JWT signing key. The fail-fast startup check moves to whatever secrets remain.
