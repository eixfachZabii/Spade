package com.spade.domain.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * lucabzt/Spade@1510db9 server/src/tests/test_analysis/test_poker_winner_analyzer.py, one row per test.
 * One row was corrected: one_pair_three_winners dealt 10S to Alice and Charlie; Charlie holds 10D here,
 * and the original deal is kept below as a test that a card dealt twice is refused.
 */
class ShowdownTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "royal_flush_vs_four_of_a_kind    | QS JS 10S 4D 4S  | Alice=AS KS; Bob=4H 4C                                   | Alice",
        "full_house_split_pot             | KS KH KD 2C 4S   | Alice=3S 3H; Bob=3D 3C                                   | Alice,Bob",
        "straight_flush_vs_flush          | 7C 8C 9C 2H 3H   | Alice=6C 5C; Bob=KH 9H                                   | Alice",
        "high_card_split_pot              | 4H 5C 6D 7C 8H   | Alice=AH 2C; Bob=AS 3D                                   | Alice,Bob",
        "flush_vs_two_pair                | KH QH JH 2C 3S   | Alice=AH 5H; Bob=10S 10C                                 | Alice",
        "full_house_vs_straight           | KD 9S 9H 8C 7H   | Alice=KS KH; Bob=9C 10S                                  | Alice",
        "three_of_a_kind_vs_two_pair      | QS 10D 10H 2C 3S | Alice=QH QD; Bob=KS KC                                   | Alice",
        "full_house_vs_flush              | JC 10C 7C 9H 9S  | Alice=JS JH; Bob=9C 3C                                   | Alice",
        "straight_vs_three_of_a_kind      | 3D 5H 4C 7S 8H   | Alice=5S 6C; Bob=4H 4S                                   | Alice",
        "one_pair_vs_high_card            | 3H 7D 9C JS QH   | Alice=10S 10D; Bob=AC KS                                 | Alice",
        "two_pair_split_pot               | 10S 10C 3H 4S 6C | Alice=JS 5H; Bob=JC 5D                                   | Alice,Bob",
        "straight_vs_two_pair             | 8C 5S 4D 7C 10H  | Alice=6H 7S; Bob=9C 9D                                   | Alice",
        "low_straight_split_pot           | 3S 4H 5D 7C 9H   | Alice=AS 2H; Bob=AD 2C                                   | Alice,Bob",
        "four_way_split_pot_with_straight | 5S 6H 7C 8D 9H   | Alice=2S 3H; Bob=2C 3D; Charlie=4S 4C; Diana=6C 6D       | Alice,Bob,Charlie,Diana",
        "one_pair_single_winner           | AH JH 7C 5D 2H   | Alice=AS KH; Bob=KC QD; Charlie=JS 10C; Diana=9C 8D      | Alice",
        "one_pair_two_winners             | 9H 5H 10C JD QH  | Alice=9S 3H; Bob=9C 4D; Charlie=5S 2C; Diana=6C 2D       | Alice,Bob",
        "one_pair_three_winners           | 9S 10H JC AD KH  | Alice=10S 4H; Bob=10C 2D; Charlie=10D 3C; Diana=6C 8D    | Alice,Bob,Charlie",
        "one_pair_kicker_decides          | 2S QH 3C 9D 8H   | Alice=QS AH; Bob=QC AD; Charlie=AS 5C; Diana=2C 6D       | Alice,Bob",
        "one_pair_kicker_two_players      | 10S 7H 8C 9D 5H  | Alice=7S 10H; Bob=7C 10D; Charlie=AS 2C; Diana=3C 4D     | Alice,Bob",
        "flush_split                      | 10S 7S 8S 9S KS  | Alice=2S 5S; Bob=4S 3S                                   | Alice,Bob",
        "high_split                       | QC 7C 6S 9S KH   | Alice=2S 8S; Bob=4S 8D                                   | Alice,Bob",
    })
    void decidesThePredecessorsShowdowns(String name, String board, String seats, String winners) {
        assertThat(Showdown.winners(Card.parseAll(board), hands(seats)))
                .containsExactly(winners.split(","));
    }

    @Test
    void refusesACardDealtTwice() {
        // the predecessor's original one_pair_three_winners: Alice and Charlie both hold 10S
        assertThatThrownBy(() -> Showdown.winners(Card.parseAll("9S 10H JC AD KH"),
                hands("Alice=10S 4H; Bob=10C 2D; Charlie=10S 3C; Diana=6C 8D")))
                .isInstanceOf(DomainException.class).hasMessageContaining("10S");
    }

    @Test
    void refusesAnIncompleteBoardOrHand() {
        assertThatThrownBy(() -> Showdown.winners(Card.parseAll("QS JS 10S 4D"), hands("Alice=AS KS; Bob=4H 4C")))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 board cards");
        assertThatThrownBy(() -> Showdown.winners(Card.parseAll("QS JS 10S 4D 4S"), hands("Alice=AS; Bob=4H 4C")))
                .isInstanceOf(DomainException.class).hasMessageContaining("2 hole cards");
    }

    private static Map<String, List<Card>> hands(String seats) {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        Arrays.stream(seats.split(";")).map(String::strip).forEach(seat -> {
            String[] parts = seat.split("=");
            hands.put(parts[0].strip(), Card.parseAll(parts[1]));
        });
        return hands;
    }
}
