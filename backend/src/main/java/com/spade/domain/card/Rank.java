package com.spade.domain.card;

/** Card ranks with their value (ace high = 14) and their label in the model's notation (ADR 0010). */
public enum Rank {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"),
    NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");

    private final int value;
    private final String label;

    Rank(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    static Rank fromLabel(String label) {
        for (Rank rank : values()) {
            if (rank.label.equals(label)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("unknown rank label " + label);
    }
}
