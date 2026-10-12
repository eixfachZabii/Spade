package com.spade.domain.eval;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Who wins one pot: the best hand out of the board plus each player's two hole cards. Ties split. */
public final class Showdown {

    private Showdown() {
    }

    public static <K> Set<K> winners(List<Card> board, Map<K, List<Card>> holeCards) {
        if (board.size() != 5) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "A showdown needs all 5 board cards, got " + board.size() + ".");
        }
        if (holeCards.isEmpty()) {
            throw new DomainException(DomainException.Kind.INVALID, "A showdown needs at least one hand.");
        }
        Set<Card> dealt = new HashSet<>();
        for (Card card : board) {
            requireFirstTime(dealt, card);
        }
        Map<K, HandRank> ranks = new LinkedHashMap<>();
        for (Map.Entry<K, List<Card>> hand : holeCards.entrySet()) {
            if (hand.getValue().size() != 2) {
                throw new DomainException(DomainException.Kind.INVALID,
                        hand.getKey() + " needs 2 hole cards, got " + hand.getValue().size() + ".");
            }
            for (Card card : hand.getValue()) {
                requireFirstTime(dealt, card);
            }
            List<Card> seven = new ArrayList<>(board);
            seven.addAll(hand.getValue());
            ranks.put(hand.getKey(), HandEvaluator.evaluate(seven));
        }
        long best = ranks.values().stream().mapToLong(HandRank::value).max().orElseThrow();
        Set<K> winners = new LinkedHashSet<>();
        ranks.forEach((key, rank) -> {
            if (rank.value() == best) {
                winners.add(key);
            }
        });
        return winners;
    }

    private static void requireFirstTime(Set<Card> dealt, Card card) {
        if (!dealt.add(card)) {
            throw new DomainException(DomainException.Kind.INVALID, card + " is dealt twice. Correct the cards first.");
        }
    }
}
