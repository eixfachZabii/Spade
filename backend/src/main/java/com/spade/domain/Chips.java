package com.spade.domain;

/** Amounts at the table are points a stack of real chips can hold: the smallest chip is 5 (ADR 0006). */
public final class Chips {

    public static final long SMALLEST = 5;
    public static final long MAX = 1_000_000;

    private Chips() {
    }

    /** A buy-in, a top-up or a blind: more than nothing. */
    public static long requireStake(long points, String what) {
        if (points <= 0) {
            throw invalid(what + " must be more than 0 points.");
        }
        return requireCountable(points, what);
    }

    /** A counted cash-out: may be nothing. */
    public static long requireCount(long points, String what) {
        if (points < 0) {
            throw invalid(what + " can't be negative.");
        }
        return requireCountable(points, what);
    }

    private static long requireCountable(long points, String what) {
        if (points % SMALLEST != 0) {
            throw invalid(what + " must be in steps of " + SMALLEST + " points (the smallest chip), got " + points + ".");
        }
        if (points > MAX) {
            throw invalid(what + " of " + points + " points is more than " + MAX + " (€10,000). A typo?");
        }
        return points;
    }

    private static DomainException invalid(String message) {
        return new DomainException(DomainException.Kind.INVALID, message);
    }
}
