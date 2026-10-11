package com.spade.domain.eval;

import java.util.List;

/** A hand's strength: its category and the five card values that decide ties, strongest first. */
public record HandRank(Category category, List<Integer> deciders) implements Comparable<HandRank> {

    public HandRank {
        deciders = List.copyOf(deciders);
        if (deciders.size() != 5) {
            throw new IllegalArgumentException("a hand rank has five deciders, got " + deciders);
        }
    }

    /** category * 100^5 + the deciders, two digits each (the encoding of the V0 vectors). */
    public long value() {
        long value = category.code();
        for (int decider : deciders) {
            value = value * 100 + decider;
        }
        return value;
    }

    @Override
    public int compareTo(HandRank other) {
        return Long.compare(value(), other.value());
    }
}
