# Card detection (`cv/`): status quo, 2026-10-09

> Dated snapshot, not maintained (V0 spec §5).
> **Changed since:** Phase 02: imported into this repo as `cv/` (snapshot of `lucabzt/spadeAI@9e4ec5e`), uv project, model in Git LFS, smoke tests.
>
> Evidence: [audit](../handoffs/done/Version0.0/V0_audits/audit-lineage.md) §C. Salvage verdicts: [salvage.md](salvage.md).

## What it does
A Flask-SocketIO service (`cv/app.py`, port 5001, plain HTTP) with two jobs:
- **Hole cards from a phone frame** (`frame` event): YOLOv8 inference on a JPEG, duplicate labels removed (a card is often detected once per printed corner), cut to `n`. This works.
- **The board from an overhead camera** (`comm_cards` event): **a stub that always answers `["QS","AS","KS"]`.** Community-card detection was never built.

It also serves the cropped table frame (`getFrame`) and recalibrates the crop (`recalibrate`). The full event contract is in [`cv/README.md`](../../cv/README.md).

## Model
- `cv/models/best_60_23.pt`: YOLOv8s fine-tuned for 60 epochs (ultralytics 8.3.78, trained 2025-02-23), stored in Git LFS.
- 52 classes named rank+suit: `2C` … `10S` … `JD`, `QH`, `KS`, `AC`. The smoke test pins the exact set.
- **Accuracy has never been measured** on our cards, light or phones. Training data lives in `lucabzt/SpadeClassifier` (not local).

## Camera calibration
`cv/camera.py` opens camera 0 on the machine running the service, finds two red spade symbols on the table mat (HSV thresholds) and crops between them with 5% padding. It also detects the yellow card-slot outlines but does nothing with them. This assumes a specific mat with five yellow boxes between two red ♠ symbols.

## How the legacy apps call it, and why that fails
`webapp/src/App.js` opens its socket.io connection to the backend's origin (`:8080`), not to `:5001`. Spring speaks STOMP, not socket.io, so `frame`, `getFrame` and `recalibrate` never reach this service. Scanned cards were never sent on to the engine either. Phones also refuse the camera on a plain-HTTP page.

## What V1 must decide (issues labelled `area:cv`)
- Can a table camera read the board reliably under real poker-night light? (the spike)
- Hole cards on the phone itself (Core ML export of this model) or on a server?
- One card notation across cv, backend, hub and iOS.
