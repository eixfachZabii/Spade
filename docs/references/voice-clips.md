# Voice-dealer clips: manifest

> **Origin:** `lucabzt/Spade@1510db9:server/assets/sounds/` (local: `~/PycharmProjects/Spade/server/assets/sounds/`). Recorded with the ElevenLabs voice "Daniel".
> **Manifest as of 2026-10-09** (`find … -name '*.mp3' | wc -l` per folder). The clips are **not in this repo**: they include friends' names. Whether Spade gets a voice, and whether it uses these clips or live TTS, is an open issue.

| Folder | Clips | Contents |
|---|---|---|
| `dealer-lines` | 49 | initiation, phrases, round start, showdown, community-card calls |
| `player-actions` | 72 | check / call / raise / fold / all-in lines |
| `players` | 54 | name call-outs and greetings, per person (names not listed here) |
| `poker-cards` | 194 | card and rank names ("ace of spades", "kings") |
| `winning-hands` | 50 | hand descriptions and winner lines ("full house, kings over …") |
| `LUSTIG` | 9 | humour lines |
| (loose files) | 8 | placeholders: `CALL TODO.mp3`, `CHECK TODO.mp3`, `FLOP TODO.mp3`, `FOLD TODO.mp3`, `RAISE TODO.mp3`, `RIVER TODO.mp3`, `TURN TODO.mp3`, `WINS TODO.mp3` |
| **Total** | **436** | |

Two-pair announcements were never recorded ([lineage audit](../handoffs/done/Version0.0/V0_audits/audit-lineage.md) §D). The Python player that sequenced them lives at `lucabzt/Spade@1510db9:server/src/mediaplayer/sound_manager.py`.
