# Hand-evaluation test vectors

## The living vectors
[`spadeboot/src/test/java/com/spadeboot/domain/game/HandEvaluationTest.java`](../../spadeboot/src/test/java/com/spadeboot/domain/game/HandEvaluationTest.java): 18 seven-card hands, one per row, each with the expected rank number (`category × 100⁵ + five deciding values`). They cover every category plus ace-low straights, a paired card inside a straight, a six-card flush, a flush hiding a straight, two trips, trips with two pairs, and three pairs. Written in V0 Phase 03; each value was checked against the compiled evaluator before and after the fix. **The rebuilt engine must pass them.**

## Reference suites from the Python engine
`lucabzt/Spade@1510db9:server/src/tests/test_analysis/` (local `~/PycharmProjects/Spade/server/src/tests/test_analysis/`). Counts as of 2026-10-09, from `grep -c 'def test'`.

**`test_poker_hand_analyzer.py` (19): best-hand category from seven cards**
`test_royal_flush`, `test_royal_flush_edge_case`, `test_royal_flush_invalid`, `test_royal_flush_missing_card`, `test_straight_flush`, `test_straight_flush_with_ace_low`, `test_straight_flush_with_flush`, `test_straight_flush_invalid`, `test_four_of_a_kind`, `test_four_of_a_kind_with_pair`, `test_four_of_a_kind_invalid`, `test_full_house`, `test_full_house_invalid`, `test_flush`, `test_flush_with_straight`, `test_flush_invalid`, `test_straight`, `test_straight_with_ace_high`, `test_straight_High`.

**`test_poker_winner_analyzer.py` (21): who wins, including splits and kickers**
`test_royal_flush_vs_four_of_a_kind`, `test_full_house_split_pot`, `test_straight_flush_vs_flush`, `test_high_card_split_pot`, `test_flush_vs_two_pair`, `test_full_house_vs_straight`, `test_three_of_a_kind_vs_two_pair`, `test_full_house_vs_flush`, `test_straight_vs_three_of_a_kind`, `test_one_pair_vs_high_card`, `test_two_pair_split_pot`, `test_straight_vs_two_pair`, `test_low_straight_split_pot`, `test_four_way_split_pot_with_straight`, `test_one_pair_single_winner`, `test_one_pair_two_winners`, `test_one_pair_three_winners`, `test_one_pair_kicker_decides`, `test_one_pair_kicker_two_players`, `test_flush_split`, `test_high_split`.

The winner suite is the more valuable one for V1: split pots and kicker decisions are exactly what the showdown must get right. Port its cases as rows, not its code.

## Dropped
The Java `HandEvaluatorTest` on the old `add_first_game_logic` branch (22 methods) had **no assertions** and never called the evaluator, so it was not restored (V0 correction C1). The branch is archived locally in `~/spade-archive/`.
