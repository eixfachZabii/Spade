package com.spade.domain.card;

import com.spade.domain.DomainException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One playing card. Written everywhere as the model writes it: "AS", "10H" (ADR 0010). */
public record Card(Rank rank, Suit suit) {

    private static final Pattern LABEL = Pattern.compile("(10|[2-9JQKA])([CDHS])");

    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    public static Card parse(String label) {
        Matcher m = label == null ? null : LABEL.matcher(label);
        if (m == null || !m.matches()) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "Not a card: '" + label + "'. Write the rank, then the suit, in capitals: AS, 10H, QD.");
        }
        return new Card(Rank.fromLabel(m.group(1)), Suit.fromLabel(m.group(2).charAt(0)));
    }

    /** Parses space-separated labels, e.g. "10H AS QC". */
    public static List<Card> parseAll(String labels) {
        return Arrays.stream(labels.strip().split("\\s+")).map(Card::parse).toList();
    }

    public static List<Card> deck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(rank, suit));
            }
        }
        return List.copyOf(deck);
    }

    public String label() {
        return rank.label() + suit.label();
    }

    @Override
    public String toString() {
        return label();
    }
}
