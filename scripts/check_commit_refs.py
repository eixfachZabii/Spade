#!/usr/bin/env python3
"""A negated close-keyword still closes the issue. Catch it before the push.

Copied from WealthWatcher (eixfachZabii/WealthWatchter) on 2026-10-09; its incident notes are WealthWatcher's.

GitHub closes an issue when a commit reaching the default branch contains
`<keyword> #N`, where the keyword is one of close/closes/closed, fix/fixes/fixed,
resolve/resolves/resolved. **The regex does not read the word in front of it.**

So the good-practice sentence the `ship-phase` ritual asks every author to write
-- record what you found and are deliberately leaving alone -- closes the very
issue it says it is leaving alone. It happened on 2026-08-23: merge `0a37269`
ended with a paragraph naming two deferred issues, one of them preceded by a
keyword and one not. The one with the keyword was silently closed and sat closed
for two days; the control, in the same sentence, stayed open.

**Why this narrow rule and not a general one.** Asking "did the author mean to
close this?" is unanswerable, which is why issue #28 judged a hook not worth
building. Asking "is there a negation in front of the keyword?" is decidable, and
it separates the real cases perfectly: measured across all 1695 commits in this
repo on 2026-08-25, four keyword references were intentional closes and exactly
one was negated -- the accident. No false positives, one true positive.

The safe phrasings, none of which contain a keyword adjacent to the reference:

    Filed, not addressed here: #18
    Deferred: #18
    Found and left alone: #18
    https://github.com/eixfachZabii/Spade/issues/18   (URL form never auto-closes)

Runs in `scripts/gate.sh`, which the ritual re-runs on the merged result *before*
`git push` -- the last moment the message can still be amended, since nothing
reaches GitHub until then.

    python3 scripts/check_commit_refs.py              # unpushed commits
    python3 scripts/check_commit_refs.py --range A..B # an explicit range
    python3 scripts/check_commit_refs.py --all        # the whole history (audit)
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys

# GitHub's closing keywords, verbatim from its docs. `:?` because `Fixes: #1`
# closes just as `Fixes #1` does.
KEYWORD = r"clos(?:e|es|ed)|fix(?:|es|ed)|resolv(?:e|es|ed)"
REFERENCE = re.compile(rf"\b({KEYWORD})\b\s*:?\s+#(\d+)", re.IGNORECASE)

# A negation anywhere in the few words before the keyword. "not fixed", "never
# resolved", "isn't fixed", "without fixing" -- all of them mean the opposite of
# what GitHub is about to do.
# `n't` carries no word boundary on its left ("isn't" is one word to the regex
# engine), so it is matched as a suffix rather than as a word. `un` is not here:
# "unfixed" has no boundary before "fix", so KEYWORD never matches it anyway.
NEGATION = re.compile(r"(?:\b(?:not|never|neither|nor|without)\b|n't)\s*$", re.IGNORECASE)
NEGATION_WINDOW = 24  # characters back from the keyword; "and not " fits, a clause does not

# A control byte cannot travel in argv (`ValueError: embedded null byte`), so the
# field separator is a text sentinel no commit message would contain.
FIELD = "<|spade-field|>"
RECORD = "<|spade-commit|>"


def _git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, text=True, check=True
    ).stdout


def default_range() -> str | None:
    """The commits that have not reached the remote yet -- the ones still amendable."""
    for candidate in ("@{upstream}..HEAD", "origin/master..HEAD"):
        try:
            _git("rev-list", "--count", candidate)
            return candidate
        except subprocess.CalledProcessError:
            continue
    return None


def commits(rev_range: str | None) -> list[tuple[str, str, str]]:
    args = ["log", f"--format=%h{FIELD}%s{FIELD}%B{RECORD}"]
    args.append("--all" if rev_range is None else rev_range)
    raw = _git(*args)
    out = []
    for block in raw.split(RECORD):
        parts = block.strip("\n").split(FIELD)
        if len(parts) >= 3 and parts[0].strip():
            out.append((parts[0].strip(), parts[1], parts[2]))
    return out


def negated_references(body: str) -> list[tuple[str, str, str]]:
    """(keyword, issue number, the phrase as written) for each negated reference."""
    found = []
    for match in REFERENCE.finditer(body):
        window = body[max(0, match.start() - NEGATION_WINDOW) : match.start()]
        # Collapse newlines so a negation wrapped across a line still counts.
        if NEGATION.search(" ".join(window.split())):
            start = max(0, match.start() - NEGATION_WINDOW)
            found.append(
                (match.group(1), match.group(2), " ".join(body[start : match.end()].split()))
            )
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--range", dest="rev_range", help="an explicit git range, e.g. master..HEAD")
    parser.add_argument("--all", action="store_true", help="scan every commit in the repo")
    args = parser.parse_args()

    if args.all:
        rev_range = None
    elif args.rev_range:
        rev_range = args.rev_range
    else:
        rev_range = default_range()
        if rev_range is None:
            print("check_commit_refs: no upstream to compare against — nothing to check")
            return 0

    try:
        scanned = commits(rev_range)
    except subprocess.CalledProcessError as exc:
        print(f"check_commit_refs: git failed — {exc.stderr.strip()}", file=sys.stderr)
        return 1

    offences = [
        (sha, subject, hit) for sha, subject, body in scanned for hit in negated_references(body)
    ]

    if not offences:
        print(f"check_commit_refs: {len(scanned)} commit(s) scanned, no negated close-keyword")
        return 0

    print("check_commit_refs: a close-keyword is about to close an issue you said you did NOT close\n")
    for sha, subject, (keyword, number, phrase) in offences:
        print(f"  {sha}  {subject[:64]}")
        print(f"      …{phrase}…")
        print(f"      GitHub reads this as `{keyword} #{number}` and will close #{number} on push.\n")
    print("  Rewrite with no keyword next to the reference — `Filed, not addressed here: #N`,")
    print("  `Deferred: #N`, or the full issue URL, which never auto-closes.")
    print("  Amend with `git commit --amend` (or `git rebase -i`) — nothing has reached GitHub yet.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
