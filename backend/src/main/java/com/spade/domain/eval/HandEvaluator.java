package com.spade.domain.eval;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import com.spade.domain.card.Suit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The best five-card hand out of 5 to 7 cards. A pure function: it never touches its argument. */
public final class HandEvaluator {

    private HandEvaluator() {
    }

    public static HandRank evaluate(Collection<Card> cards) {
        List<Card> hand = List.copyOf(cards);
        if (hand.size() < 5 || hand.size() > 7) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "A hand is judged from 5 to 7 cards, got " + hand.size() + ".");
        }
        if (new HashSet<>(hand).size() != hand.size()) {
            throw new DomainException(DomainException.Kind.INVALID, "A card appears twice in " + hand + ".");
        }

        Map<Suit, List<Integer>> bySuit = new EnumMap<>(Suit.class);
        int[] count = new int[15];
        for (Card card : hand) {
            bySuit.computeIfAbsent(card.suit(), s -> new ArrayList<>()).add(card.rank().value());
            count[card.rank().value()]++;
        }

        List<Integer> flush = null;
        for (List<Integer> suited : bySuit.values()) {
            if (suited.size() >= 5) {
                flush = suited.stream().sorted(Comparator.reverseOrder()).toList();
            }
        }
        if (flush != null) {
            int high = straightHigh(flush);
            if (high > 0) {
                return new HandRank(Category.STRAIGHT_FLUSH, straight(high));
            }
        }

        List<Integer> quads = atLeast(count, 4);
        if (!quads.isEmpty()) {
            int q = quads.getFirst();
            return new HandRank(Category.FOUR_OF_A_KIND, List.of(q, q, q, q, kickers(count, Set.of(q), 1).getFirst()));
        }

        List<Integer> trips = atLeast(count, 3);
        if (!trips.isEmpty()) {
            int t = trips.getFirst();
            for (int v = 14; v >= 2; v--) {
                if (v != t && count[v] >= 2) {
                    return new HandRank(Category.FULL_HOUSE, List.of(t, t, t, v, v));
                }
            }
        }

        if (flush != null) {
            return new HandRank(Category.FLUSH, flush.subList(0, 5));
        }

        int straightHigh = straightHigh(distinctValues(count));
        if (straightHigh > 0) {
            return new HandRank(Category.STRAIGHT, straight(straightHigh));
        }

        if (!trips.isEmpty()) {
            int t = trips.getFirst();
            List<Integer> k = kickers(count, Set.of(t), 2);
            return new HandRank(Category.THREE_OF_A_KIND, List.of(t, t, t, k.get(0), k.get(1)));
        }

        List<Integer> pairs = atLeast(count, 2);
        if (pairs.size() >= 2) {
            int a = pairs.get(0);
            int b = pairs.get(1);
            return new HandRank(Category.TWO_PAIR, List.of(a, a, b, b, kickers(count, Set.of(a, b), 1).getFirst()));
        }
        if (pairs.size() == 1) {
            int p = pairs.getFirst();
            List<Integer> k = kickers(count, Set.of(p), 3);
            return new HandRank(Category.PAIR, List.of(p, p, k.get(0), k.get(1), k.get(2)));
        }
        return new HandRank(Category.HIGH_CARD, kickers(count, Set.of(), 5));
    }

    /** Values held at least n times, highest first. */
    private static List<Integer> atLeast(int[] count, int n) {
        List<Integer> values = new ArrayList<>();
        for (int v = 14; v >= 2; v--) {
            if (count[v] >= n) {
                values.add(v);
            }
        }
        return values;
    }

    /** The n highest distinct values not in {@code exclude}. */
    private static List<Integer> kickers(int[] count, Set<Integer> exclude, int n) {
        List<Integer> values = new ArrayList<>();
        for (int v = 14; v >= 2 && values.size() < n; v--) {
            if (count[v] > 0 && !exclude.contains(v)) {
                values.add(v);
            }
        }
        return values;
    }

    private static List<Integer> distinctValues(int[] count) {
        return atLeast(count, 1);
    }

    /** The high card of the best straight in these values (5 for A-2-3-4-5), or 0 if there is none. */
    private static int straightHigh(List<Integer> values) {
        boolean[] present = new boolean[15];
        for (int v : values) {
            present[v] = true;
        }
        present[1] = present[14];
        for (int high = 14; high >= 5; high--) {
            if (present[high] && present[high - 1] && present[high - 2] && present[high - 3] && present[high - 4]) {
                return high;
            }
        }
        return 0;
    }

    /** Deciders of a straight; in A-2-3-4-5 the ace goes last and stays 14 (the V0 vectors' encoding). */
    private static List<Integer> straight(int high) {
        return high == 5 ? List.of(5, 4, 3, 2, 14) : List.of(high, high - 1, high - 2, high - 3, high - 4);
    }
}
