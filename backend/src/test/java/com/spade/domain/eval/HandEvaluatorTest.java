package com.spade.domain.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Rank number = category * 100^5 + the five deciding values, two digits each. Categories: 9 straight flush,
 * 8 quads, 7 full house, 6 flush, 5 straight, 4 trips, 3 two pair, 2 pair, 1 high card. Ace = 14; in an
 * ace-low straight the ace moves to the end, still 14, so 5-4-3-2-A ranks below 6-high.
 * The first suite is V0 Phase 03's 18 vectors (spadeboot HandEvaluationTest), rewritten in the model's notation.
 */
class HandEvaluatorTest {

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
        "royal flush                      | 10H JH QH KH AH 9D 8C  | 91413121110",
        "straight flush, 9 high           | 5C 6C 7C 8C 9C KD AH   | 90908070605",
        "ace-low straight flush           | AD 2D 3D 4D 5D KC QH   | 90504030214",
        "four of a kind                   | AS AH AD AC KD 2C 3H   | 81414141413",
        "full house                       | KS KH KD 9C 9D 2S 3H   | 71313130909",
        "full house from two trips        | 9S 9H 9D KC KD KS 2H   | 71313130909",
        "full house: trips + two pairs    | 5S 5H 5D KC KD 2S 2H   | 70505051313",
        "flush                            | 2H 7H 9H JH KH 3D 4C   | 61311090702",
        "flush of six takes the top five  | 2H 7H 9H JH KH AH 4C   | 61413110907",
        "flush beats the straight inside  | 4H 5H 6H 7D 8H KH 2C   | 61308060504",
        "straight                         | 6H 7D 8C 9S 10D 2C 2H  | 51009080706",
        "ace-low straight                 | AH 2D 3C 4S 5H 9D KC   | 50504030214",
        "straight with a paired card      | 5H 6D 6C 7S 8D 9C KH   | 50908070605",
        "three of a kind                  | 7S 7H 7D AC KD 2S 3H   | 40707071413",
        "two pair, ace kicker             | JS JH 4D 4C AD 2S 3H   | 31111040414",
        "three pairs: best two + kicker   | AS AH KD KC QS QH 2D   | 31414131312",
        "pair                             | 10S 10H 4D 8C AD 2S 3H | 21010140804",
        "high card                        | 2S 4H 7D 9C JD QS AH   | 11412110907",
    })
    void ranksTheV0Vectors(String description, String hand, long expected) {
        assertThat(HandEvaluator.evaluate(Card.parseAll(hand)).value()).isEqualTo(expected);
    }

    /**
     * lucabzt/Spade@1510db9 server/src/tests/test_analysis/test_poker_hand_analyzer.py, every test that asserts
     * something. Six of its tests (royal_flush_invalid, royal_flush_missing_card, straight_flush_invalid,
     * four_of_a_kind_invalid, full_house_invalid, flush_invalid) compare against a tuple shape get_best_hand
     * never returns, so they can't fail; they are not ported (V0 lesson C1).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "royal_flush                 | 10H JH QH KH AH 9D 8C    | STRAIGHT_FLUSH | 14",
        "royal_flush_edge_case       | AH KH QH JH 10H AD AC    | STRAIGHT_FLUSH | 14",
        "straight_flush              | 9H 10H JH QH KH AD AC    | STRAIGHT_FLUSH | 13",
        "straight_flush_with_ace_low | AH 2H 3H 4H 5H 6D 7C     | STRAIGHT_FLUSH | 5",
        "straight_flush_with_flush   | 9H 10H JH QH KH AD AC    | STRAIGHT_FLUSH | 13",
        "four_of_a_kind              | 10H 10D 10C 10S AH KD QC | FOUR_OF_A_KIND | 10",
        "four_of_a_kind_with_pair    | 10H 10D 10C 10S AH AD QC | FOUR_OF_A_KIND | 10",
        "full_house                  | 10H 10D 10C 9S 9H AD QC  | FULL_HOUSE     | 10",
        "flush                       | 10H JH QC KH AH 9D 8H    | FLUSH          | 14",
        "flush_with_straight         | 10H JH QH KH 9D 2H AS    | FLUSH          | 13",
        "straight                    | 9H 10D JC QS KH 2D 8C    | STRAIGHT       | 13",
        "straight_with_ace_high      | 10H AH QC KS 2D 9D JC    | STRAIGHT       | 14",
        "straight_High               | 10H JH QH KH AD 9S 8C    | STRAIGHT       | 14",
    })
    void agreesWithThePredecessorsAnalyzer(String name, String hand, Category category, int topDecider) {
        HandRank rank = HandEvaluator.evaluate(Card.parseAll(hand));
        assertThat(rank.category()).isEqualTo(category);
        assertThat(rank.deciders().getFirst()).isEqualTo(topDecider);
    }

    @Test
    void neverReordersTheCallersList() {
        List<Card> hand = new ArrayList<>(Card.parseAll("2S 4H 7D 9C JD QS AH"));
        List<Card> before = List.copyOf(hand);
        HandEvaluator.evaluate(hand);
        assertThat(hand).isEqualTo(before);
    }

    @Test
    void comparesHandsByStrength() {
        HandRank wheel = HandEvaluator.evaluate(Card.parseAll("AH 2D 3C 4S 5H 9D KC"));
        HandRank sixHigh = HandEvaluator.evaluate(Card.parseAll("6H 2D 3C 4S 5H 9D KC"));
        assertThat(sixHigh).isGreaterThan(wheel);
    }

    @Test
    void refusesTooFewTooManyOrRepeatedCards() {
        assertThatThrownBy(() -> HandEvaluator.evaluate(Card.parseAll("AS KS QS JS")))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 to 7 cards");
        assertThatThrownBy(() -> HandEvaluator.evaluate(Card.parseAll("AS KS QS JS 10S 9S 8S 7S")))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 to 7 cards");
        assertThatThrownBy(() -> HandEvaluator.evaluate(Card.parseAll("AS AS QS JS 10S")))
                .isInstanceOf(DomainException.class).hasMessageContaining("twice");
    }
}
