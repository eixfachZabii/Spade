package com.spadeboot.domain.game;

import com.spadeboot.domain.card.Card;
import com.spadeboot.domain.card.Suit;
import com.spadeboot.domain.card.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Seven-card hands against their rank number:
 * category * 100^5 + the five deciding card values, two digits each.
 * Categories: 9 straight flush, 8 quads, 7 full house, 6 flush, 5 straight,
 * 4 trips, 3 two pair, 2 pair, 1 high card. Ace = 14. In an ace-low straight the
 * ace moves to the end, still written 14 (so 5-4-3-2-A ranks below 6-high).
 * Card notation here: rank 2-9 T J Q K A + suit h d c s.
 */
class HandEvaluationTest {

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
        "royal flush                      | Th Jh Qh Kh Ah 9d 8c | 91413121110",
        "straight flush, 9 high           | 5c 6c 7c 8c 9c Kd Ah | 90908070605",
        "ace-low straight flush           | Ad 2d 3d 4d 5d Kc Qh | 90504030214",
        "four of a kind                   | As Ah Ad Ac Kd 2c 3h | 81414141413",
        "full house                       | Ks Kh Kd 9c 9d 2s 3h | 71313130909",
        "full house from two trips        | 9s 9h 9d Kc Kd Ks 2h | 71313130909",
        "full house: trips + two pairs    | 5s 5h 5d Kc Kd 2s 2h | 70505051313",
        "flush                            | 2h 7h 9h Jh Kh 3d 4c | 61311090702",
        "flush of six takes the top five  | 2h 7h 9h Jh Kh Ah 4c | 61413110907",
        "flush beats the straight inside  | 4h 5h 6h 7d 8h Kh 2c | 61308060504",
        "straight                         | 6h 7d 8c 9s Td 2c 2h | 51009080706",
        "ace-low straight                 | Ah 2d 3c 4s 5h 9d Kc | 50504030214",
        "straight with a paired card      | 5h 6d 6c 7s 8d 9c Kh | 50908070605",
        "three of a kind                  | 7s 7h 7d Ac Kd 2s 3h | 40707071413",
        "two pair, ace kicker             | Js Jh 4d 4c Ad 2s 3h | 31111040414",
        "three pairs: best two + kicker   | As Ah Kd Kc Qs Qh 2d | 31414131312",
        "pair                             | Ts Th 4d 8c Ad 2s 3h | 21010140804",
        "high card                        | 2s 4h 7d 9c Jd Qs Ah | 11412110907",
    })
    void ranksSevenCardHands(String description, String hand, long expected) {
        assertEquals(expected, HandEvaluation.cardsToRankNumber(cards(hand)));
    }

    static List<Card> cards(String hand) {
        List<Card> cards = new ArrayList<>();
        for (String token : hand.trim().split("\\s+")) {
            Card card = new Card();
            card.setValue(value(token.substring(0, token.length() - 1)));
            card.setSuit(suit(token.charAt(token.length() - 1)));
            cards.add(card);
        }
        return cards;
    }

    private static Value value(String rank) {
        return switch (rank) {
            case "T" -> Value.TEN;
            case "J" -> Value.JACK;
            case "Q" -> Value.QUEEN;
            case "K" -> Value.KING;
            case "A" -> Value.ACE;
            default -> Value.values()[Integer.parseInt(rank) - 2];
        };
    }

    private static Suit suit(char suit) {
        return switch (suit) {
            case 'h' -> Suit.HEARTS;
            case 'd' -> Suit.DIAMONDS;
            case 'c' -> Suit.CLUBS;
            case 's' -> Suit.SPADES;
            default -> throw new IllegalArgumentException("unknown suit " + suit);
        };
    }
}
