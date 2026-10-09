#!/usr/bin/env python3
"""Every issue is on the "Spade" board, in the column its state says it belongs in.

GitHub's built-in "auto-add" workflow puts new issues on the board. Moving closed
issues is done here instead: WealthWatcher measured GitHub's "item closed" automation
firing zero times in eight closes (ADR 0003).

Rules:
  open issue not on the board                -> add it, Status = Inbox
  open issue with no Status, or in Shipped / WontDo (reopened) -> Inbox
  open issue in Grilling / Ready / Building  -> left alone (a person put it there)
  closed as completed (or no reason)         -> Shipped
  closed as not planned / duplicate          -> WontDo

    python3 scripts/check_board.py          # report; exit 1 if anything is off
    python3 scripts/check_board.py --fix    # repair it

GitHub's project item list is eventually consistent: right after --fix, a plain
check can still report the items it just fixed (measured 2026-10-09: 5–45 s).
Wait up to a minute and re-run before suspecting the script.
"""
from __future__ import annotations

import json
import subprocess
import sys
from dataclasses import dataclass

OWNER = "eixfachZabii"
REPO = "eixfachZabii/Spade"
PROJECT_TITLE = "Spade"
INBOX, SHIPPED, WONTDO = "Inbox", "Shipped", "WontDo"
NOT_DONE = {"NOT_PLANNED", "DUPLICATE"}


@dataclass(frozen=True)
class Issue:
    number: int
    url: str
    open: bool
    state_reason: str | None


@dataclass(frozen=True)
class Item:
    item_id: str
    number: int
    status: str | None


@dataclass(frozen=True)
class Action:
    kind: str  # "add" or "set"
    number: int
    status: str
    item_id: str | None = None
    url: str | None = None


def desired_status(issue: Issue, current: str | None) -> str | None:
    """The column the issue must be in, or None when any working column is fine."""
    if not issue.open:
        return WONTDO if (issue.state_reason or "").upper() in NOT_DONE else SHIPPED
    if current in (None, SHIPPED, WONTDO):
        return INBOX
    return None


def plan(issues: list[Issue], items: list[Item]) -> list[Action]:
    on_board = {item.number: item for item in items}
    actions: list[Action] = []
    for issue in sorted(issues, key=lambda i: i.number):
        item = on_board.get(issue.number)
        target = desired_status(issue, item.status if item else None)
        if item is None:
            actions.append(Action("add", issue.number, target or INBOX, url=issue.url))
        elif target is not None and target != item.status:
            actions.append(Action("set", issue.number, target, item_id=item.item_id))
    return actions


# ---------------------------------------------------------------- GitHub I/O


def gh(*args: str) -> str:
    return subprocess.run(["gh", *args], capture_output=True, text=True, check=True).stdout


def load_issues() -> list[Issue]:
    raw = json.loads(gh("issue", "list", "--repo", REPO, "--state", "all", "--limit", "1000",
                        "--json", "number,url,state,stateReason"))
    return [Issue(r["number"], r["url"], r["state"] == "OPEN", r.get("stateReason") or None) for r in raw]


def find_project() -> dict:
    for project in json.loads(gh("project", "list", "--owner", OWNER, "--format", "json"))["projects"]:
        if project["title"] == PROJECT_TITLE:
            return project
    raise SystemExit(f"check_board: no project titled {PROJECT_TITLE!r} for {OWNER}")


def load_items(number: int) -> list[Item]:
    raw = json.loads(gh("project", "item-list", str(number), "--owner", OWNER, "--format", "json", "--limit", "1000"))
    items = []
    for it in raw["items"]:
        content = it.get("content") or {}
        if content.get("type") == "Issue" and content.get("repository") == REPO:
            items.append(Item(it["id"], content["number"], it.get("status") or None))
    return items


def status_field(number: int) -> tuple[str, dict[str, str]]:
    for field in json.loads(gh("project", "field-list", str(number), "--owner", OWNER, "--format", "json"))["fields"]:
        if field["name"] == "Status":
            return field["id"], {o["name"]: o["id"] for o in field.get("options", [])}
    raise SystemExit("check_board: the project has no Status field")


def apply(actions: list[Action], project: dict, field_id: str, options: dict[str, str]) -> None:
    for action in actions:
        item_id = action.item_id
        if action.kind == "add":
            added = json.loads(gh("project", "item-add", str(project["number"]), "--owner", OWNER,
                                  "--url", action.url, "--format", "json"))
            item_id = added["id"]
        gh("project", "item-edit", "--id", item_id, "--project-id", project["id"],
           "--field-id", field_id, "--single-select-option-id", options[action.status])


def main() -> int:
    fix = "--fix" in sys.argv
    project = find_project()
    actions = plan(load_issues(), load_items(project["number"]))
    if not actions:
        print("  ✓ every issue is on the board in the right column")
        return 0
    for a in actions:
        print(f"  {'fixing' if fix else '✗'} #{a.number}: {a.kind} → {a.status}")
    if not fix:
        print("\n  run: python3 scripts/check_board.py --fix")
        return 1
    field_id, options = status_field(project["number"])
    missing = {a.status for a in actions} - options.keys()
    if missing:
        raise SystemExit(f"check_board: Status field lacks options {sorted(missing)}")
    apply(actions, project, field_id, options)
    print(f"  ✓ fixed {len(actions)} item(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
