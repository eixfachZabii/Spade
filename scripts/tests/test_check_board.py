"""Tests for the pure planning half of scripts/check_board.py."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from check_board import INBOX, SHIPPED, WONTDO, Action, Issue, Item, plan  # noqa: E402

URL = "https://github.com/eixfachZabii/Spade/issues/{}"


def issue(n, open_=True, reason=None):
    return Issue(number=n, url=URL.format(n), open=open_, state_reason=reason)


def test_open_issue_missing_from_board_is_added_to_inbox():
    assert plan([issue(1)], []) == [Action("add", 1, INBOX, url=URL.format(1))]


def test_open_issue_on_board_without_status_goes_to_inbox():
    assert plan([issue(2)], [Item("I2", 2, None)]) == [Action("set", 2, INBOX, item_id="I2")]


def test_open_issue_in_a_working_column_is_left_alone():
    assert plan([issue(3)], [Item("I3", 3, "Ready")]) == []


def test_reopened_issue_sitting_in_shipped_goes_back_to_inbox():
    assert plan([issue(4, reason="REOPENED")], [Item("I4", 4, SHIPPED)]) == [Action("set", 4, INBOX, item_id="I4")]


def test_closed_as_completed_goes_to_shipped():
    assert plan([issue(5, open_=False, reason="COMPLETED")], [Item("I5", 5, "Building")]) == [
        Action("set", 5, SHIPPED, item_id="I5")
    ]


def test_closed_as_not_planned_or_duplicate_goes_to_wontdo():
    actions = plan(
        [issue(6, open_=False, reason="NOT_PLANNED"), issue(7, open_=False, reason="DUPLICATE")],
        [Item("I6", 6, "Inbox"), Item("I7", 7, "Inbox")],
    )
    assert actions == [Action("set", 6, WONTDO, item_id="I6"), Action("set", 7, WONTDO, item_id="I7")]


def test_closed_issue_missing_from_board_is_added_in_its_final_column():
    assert plan([issue(8, open_=False, reason="COMPLETED")], []) == [Action("add", 8, SHIPPED, url=URL.format(8))]


def test_closed_issue_already_in_the_right_column_needs_nothing():
    assert plan([issue(9, open_=False, reason="COMPLETED")], [Item("I9", 9, SHIPPED)]) == []


def test_empty_state_reason_on_a_closed_issue_counts_as_completed():
    assert plan([issue(10, open_=False, reason="")], [Item("I10", 10, "Ready")]) == [
        Action("set", 10, SHIPPED, item_id="I10")
    ]
