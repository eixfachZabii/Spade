# References

Assets carried forward from earlier Spade repos. Each row names its origin so it can be re-checked. These are inputs, not specs: an ADR or a phase doc decides what is built. What survives of the old *code* is decided in the [salvage map](../status-quo/salvage.md).

| Asset | Origin | Used for | Caveats |
|---|---|---|---|
| [poker-chip-tracker.xlsx](poker-chip-tracker.xlsx) | the group's ledger, 2024-10-13 → 2025-05-21 (was `spadeboot/src/main/resources/`) | the ledger feature; real P&L history of 20 nights | friends' names and money |
| [legacy-api.md](legacy-api.md) | `lucabzt/Spade@1510db9` Flask, this repo's Spring code, `lucabzt/spadeAI@9e4ec5e` | what the rebuild keeps or drops | the Spring part reflects V0 |
| [user-stories.md](user-stories.md) | `lucabzt/Spade@1510db9:zDone` | requirements input for the V1 grill | rewritten; the source transcript is not reused |
| [uml/](uml/) | `lucabzt/Spade@1510db9:UML`, `zUML.txt` | the 2025 domain model and layering | both files parse with `plantuml -checkonly`; `zuml.puml` holds two diagrams |
| [hand-eval-vectors.md](hand-eval-vectors.md) | this repo's tests + `lucabzt/Spade@1510db9` Python suites | engine correctness | |
| [voice-clips.md](voice-clips.md) | `lucabzt/Spade@1510db9:server/assets/sounds/` | the voice-dealer idea | clips not in the repo |
| [webapp-style.md](webapp-style.md) | `webapp/src/styles/` (legacy phone app) | the starting point for the new visual style (D24) | extracted values, not a design system |

Not here on purpose:
- the table reference photos in `lucabzt/Spade:server/src/classifier/table/images/` (they include personal photo thumbnails);
- the YOLO model (it lives in `cv/models/`);
- the voice clips themselves (friends' names).
