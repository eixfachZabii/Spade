# cv: Spade card detection

Spade's eyes. A Flask-SocketIO service that reads playing cards with a YOLOv8 model.

**Provenance.** Snapshot of [`lucabzt/spadeAI@9e4ec5e`](https://github.com/lucabzt/spadeAI/tree/9e4ec5e) (2025-03-24), written by Luca Bozzetti and Sebastian Rogg. Copied without history (V0 spec D4). Three changes at import: the model path is resolved relative to this file, two unused imports were removed, and a one-line `if … : break` was split.

## Run
    uv sync                      # first run downloads torch
    uv run python app.py         # socket.io on 0.0.0.0:5001, opens camera index 0
    uv run pytest -q             # smoke tests (no camera needed)

The model `models/best_60_23.pt` is stored in Git LFS. If tests say "LFS pointer", run `git lfs pull`.

## Contract (socket.io, answers via the ack callback)
| Event | Request | Ack |
|---|---|---|
| `frame` | `{n: int, image: ArrayBuffer (JPEG)}` | `{predictions: [label…], found: bool}`: duplicates removed, cut to `n` |
| `comm_cards` | `{n: int}` | **stub**: always `["QS","AS","KS"]` |
| `getFrame` | `{tableId}` | `{success, image: JPEG bytes}` (cropped table frame, spades boxed) |
| `recalibrate` | `{tableId}` | `{success, message?}` |

Labels are rank+suit: `2C` … `10S` … `JD`, `QH`, `KS`, `AC` (52 classes).

## Known gaps (see the issues labelled `area:cv`)
- Community-card detection was never built (`get_comm_cards` is a stub). This is the V1 spike.
- Serves plain HTTP. Phones need HTTPS for the camera.
- The legacy `webapp/` connects socket.io to the backend's port, not to `:5001`, so it never reaches this service.
- The card notation differs from the backend (`ACEH`-style) and the legacy client.
