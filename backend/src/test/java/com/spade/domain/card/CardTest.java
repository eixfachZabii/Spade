package com.spade.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.domain.DomainException;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {

    @Test
    void everyCardOfTheDeckRoundTripsThroughItsLabel() {
        List<Card> deck = Card.deck();
        assertThat(deck).hasSize(52);
        assertThat(new HashSet<>(deck)).hasSize(52);
        for (Card card : deck) {
            assertThat(Card.parse(card.label())).isEqualTo(card);
        }
    }

    @Test
    void readsTheModelsLabels() {
        assertThat(Card.parse("AS")).isEqualTo(new Card(Rank.ACE, Suit.SPADES));
        assertThat(Card.parse("10H")).isEqualTo(new Card(Rank.TEN, Suit.HEARTS));
        assertThat(Card.parse("JD").label()).isEqualTo("JD");
        assertThat(Card.parseAll(" 10H  AS QC ")).extracting(Card::label).containsExactly("10H", "AS", "QC");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"as", "1H", "TH", "11H", "10", "AX", "AAS", " AS", "AS ", "10h", "ACEH", "THREES"})
    void rejectsAnythingThatIsNotAModelLabel(String label) {
        assertThatThrownBy(() -> Card.parse(label))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Not a card")
                .extracting(e -> ((DomainException) e).kind()).isEqualTo(DomainException.Kind.INVALID);
    }
}
