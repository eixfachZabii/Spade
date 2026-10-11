package com.spade.domain.card;

/** Suits with their one-letter label in the model's notation (ADR 0010). */
public enum Suit {
    CLUBS('C'), DIAMONDS('D'), HEARTS('H'), SPADES('S');

    private final char label;

    Suit(char label) {
        this.label = label;
    }

    public char label() {
        return label;
    }

    static Suit fromLabel(char label) {
        for (Suit suit : values()) {
            if (suit.label == label) {
                return suit;
            }
        }
        throw new IllegalArgumentException("unknown suit label " + label);
    }
}
