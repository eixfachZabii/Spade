---
name: capture-idea
description: Use when the owner has an idea, feature request, bug, complaint or "wouldn't it be nice if" for Spade — anything that would otherwise be said in chat and lost. Checks whether it already exists, files it as a GitHub issue with the repo context attached, and puts it on the board.
---

# Capture an idea

An idea said in chat has no state; an issue does. WealthWatcher measured the cost: when its chat backlog moved to issues, five of eleven ideas had already shipped. So step 1 is the point.

## 1. Does it already exist?
```bash
gh issue list --repo eixfachZabii/Spade --state all --search "<two or three words>"
grep -n -i "<word>" docs/handoffs/INDEX.md
grep -rn -i "<word>" spadeboot/src cv docs/status-quo docs/references | head
```
- **An open issue covers it**: comment there with the owner's words and the date. No duplicate.
- **Already shipped**: file it anyway, then close it at once with the label `already-shipped` and a comment saying where it lives.
- **Partly there**: file it open, and say which parts exist, with paths.
- **New**: file it open.

## 2. Write it so it can be built later
```markdown
**Owner, <YYYY-MM-DD>:** *"<their words, verbatim>"*
**What exists.** <files, endpoints, docs it builds on — paths in backticks>
**The problem / idea.** <behaviour, with evidence: a command and its output, or file:line>
**Constraints.** <PRODUCT.md principles and ADRs it must respect>
**Open questions.** <what a grill has to settle>
**Not this issue:** <neighbouring issues, by URL>
```
Found by an agent rather than said by the owner? The first line becomes `**Found by:** <doc, audit section or command>`. Never name where a secret lives: issues are public.

## 3. Label it and put it on the board
Write the body to a temp file (the Write tool, or `mktemp`), then:
```bash
gh issue create --repo eixfachZabii/Spade --title "<outcome-shaped title>" \
  --label "area:<backend|cv|frontend|devex|docs|security>,type:<bug|idea|chore>,version:<v1|later>" \
  --body-file "<the temp file>"
python3 scripts/check_board.py --fix
```
Add `needs-grill` when the design is open. `type:phase` is set only by promotion (step 4).

## 4. Do not start a second roadmap
Issues are the inbox; `docs/handoffs/INDEX.md` is the roadmap (ADR 0003). An issue becomes a phase when an INDEX phase entry links it. At that point it gets `type:phase`, moves to Ready, and stays open until the phase ships. Never write a "future work" checklist in a doc.
