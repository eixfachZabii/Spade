package com.spade.domain.eval;

/** Hand categories, weakest first; {@link #code()} is the leading digit of a rank number. */
public enum Category {
    HIGH_CARD(1), PAIR(2), TWO_PAIR(3), THREE_OF_A_KIND(4), STRAIGHT(5),
    FLUSH(6), FULL_HOUSE(7), FOUR_OF_A_KIND(8), STRAIGHT_FLUSH(9);

    private final int code;

    Category(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
