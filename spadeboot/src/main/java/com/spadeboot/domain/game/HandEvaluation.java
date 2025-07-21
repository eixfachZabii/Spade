package com.spadeboot.domain.game;

import java.util.*;

import com.spadeboot.domain.card.Card;
import com.spadeboot.domain.card.CardHelper;
import com.spadeboot.domain.card.Suit;

public class HandEvaluation {
    /**
     * Computes the hand ranking as an integer based on the poker hand.
     * The ranking is calculated by checking from the highest rank (straight flush)
     * to the lowest (high card).
     */
    public static long cardsToRankNumber(List<Card> cards) {
        // sort cards descending by card getValue().getValue()
        cards.sort((a, b) -> b.getValue().getValue() - a.getValue().getValue());

        // Check for straight flush in each suit
        List<Card> spadeCards = filterBySuit(cards, "S");
        List<Integer> resultCards = getStraight(spadeCards);
        if (resultCards.size() == 5) {
            return getRankNumber(9, resultCards);
        }
        List<Card> heartCards = filterBySuit(cards, "H");
        resultCards = getStraight(heartCards);
        if (resultCards.size() == 5) {
            return getRankNumber(9, resultCards);
        }
        List<Card> clubCards = filterBySuit(cards, "C");
        resultCards = getStraight(clubCards);
        if (resultCards.size() == 5) {
            return getRankNumber(9, resultCards);
        }
        List<Card> diamondCards = filterBySuit(cards, "D");
        resultCards = getStraight(diamondCards);
        if (resultCards.size() == 5) {
            return getRankNumber(9, resultCards);
        }

        // Prepare list of card values and count occurrences
        List<Integer> cardValues = new ArrayList<>();
        for (Card card : cards) {
            cardValues.add(card.getValue().getValue());
        }
        Map<Integer, Integer> cardCounts = countOccurrences(cardValues);

        // Four of a kind
        if (cardCounts.containsValue(4)) {
            int fourCard = getKeyWithCount(cardCounts, 4);
            List<Integer> otherCardValues = new ArrayList<>();
            for (int value : cardValues) {
                if (value != fourCard) {
                    otherCardValues.add(value);
                }
            }
            return getRankNumber(8, Arrays.asList(fourCard, fourCard, fourCard, fourCard, otherCardValues.get(0)));
        }

        // Full house (a triple and a pair)
        Integer threeCard = getKeyWithCount(cardCounts, 3);
        if (threeCard != null) {
            Integer twoCard = getKeyWithCountExcluding(cardCounts, 2, threeCard);
            if (twoCard != null) {
                return getRankNumber(7, Arrays.asList(threeCard, threeCard, threeCard, twoCard, twoCard));
            }
        }

        // Flush: if any suit contains 5 or more cards, use the top five cards
        if (spadeCards.size() >= 5) {
            return getRankNumber(6, getTopValues(spadeCards, 5));
        }
        if (heartCards.size() >= 5) {
            return getRankNumber(6, getTopValues(heartCards, 5));
        }
        if (clubCards.size() >= 5) {
            return getRankNumber(6, getTopValues(clubCards, 5));
        }
        if (diamondCards.size() >= 5) {
            return getRankNumber(6, getTopValues(diamondCards, 5));
        }

        // Straight
        resultCards = getStraight(cards);
        if (resultCards.size() == 5) {
            return getRankNumber(5, resultCards);
        }

        // Three of a kind
        if (cardCounts.containsValue(3)) {
            threeCard = getKeyWithCount(cardCounts, 3);
            List<Integer> otherCardValues = new ArrayList<>();
            for (int value : cardValues) {
                if (value != threeCard) {
                    otherCardValues.add(value);
                }
            }
            return getRankNumber(4, Arrays.asList(threeCard, threeCard, threeCard, otherCardValues.get(0), otherCardValues.get(1)));
        }

        // Two pair: find two distinct pairs
        List<Integer> pairs = getKeysWithCount(cardCounts, 2);
        if (pairs.size() >= 2) {
            pairs.sort(Collections.reverseOrder());
            int firstPair = pairs.get(0);
            int secondPair = pairs.get(1);
            List<Integer> otherCardValues = new ArrayList<>();
            for (int value : cardValues) {
                if (value != firstPair && value != secondPair) {
                    otherCardValues.add(value);
                }
            }
            return getRankNumber(3, Arrays.asList(firstPair, firstPair, secondPair, secondPair, otherCardValues.get(0)));
        }

        // Pair: if any card appears twice (note: this check is reached only if not already two pairs)
        if (cardCounts.containsValue(2)) {
            int twoCard = getKeyWithCount(cardCounts, 2);
            List<Integer> otherCardValues = new ArrayList<>();
            for (int value : cardValues) {
                if (value != twoCard) {
                    otherCardValues.add(value);
                }
            }
            return getRankNumber(2, Arrays.asList(twoCard, twoCard, otherCardValues.get(0), otherCardValues.get(1), otherCardValues.get(2)));
        }

        // High card: use the top five card values from the sorted list
        List<Integer> top5 = cardValues.subList(0, Math.min(5, cardValues.size()));
        return getRankNumber(1, top5);
    }

    /**
     * Returns a list containing only the cards that match the given suit.
     */
    private static List<Card> filterBySuit(List<Card> cards, String suit) {
        List<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (card.getSuit().equals(suit)) {
                result.add(card);
            }
        }
        return result;
    }

    /**
     * Returns a list of the top 'count' card values from a sorted list of cards.
     */
    private static List<Integer> getTopValues(List<Card> cards, int count) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < count && i < cards.size(); i++) {
            values.add(cards.get(i).getValue().getValue());
        }
        return values;
    }

    /**
     * Counts the number of occurrences of each integer getValue().getValue() in the list.
     */
    private static Map<Integer, Integer> countOccurrences(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int value : values) {
            counts.put(value, counts.getOrDefault(value, 0) + 1);
        }
        return counts;
    }

    /**
     * Returns a key (card getValue().getValue()) with the given count.
     */
    private static Integer getKeyWithCount(Map<Integer, Integer> counts, int targetCount) {
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= targetCount) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns a key with the given count while excluding a specified getValue().getValue().
     */
    private static Integer getKeyWithCountExcluding(Map<Integer, Integer> counts, int targetCount, int exclude) {
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getKey() != exclude && entry.getValue() >= targetCount) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns all keys that appear exactly targetCount times.
     */
    private static List<Integer> getKeysWithCount(Map<Integer, Integer> counts, int targetCount) {
        List<Integer> keys = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == targetCount) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    /**
     * Returns a rank number based on the hand type and the list of five card values.
     * If the lowest card is an Ace (represented as 1), it is removed and a 14 is appended.
     * The final number is built by multiplying the type number by 100 and adding each card getValue().getValue().
     */
    public static long getRankNumber(int typeNumber, List<Integer> cards) {
        // Adjust Ace getValue().getValue() if it is in the 5th position (index 4)
        if (cards.size() >= 5 && cards.get(4) == 1) {
            cards.remove(Integer.valueOf(1));
            cards.add(14);
        }
        long res = typeNumber;
        for (int card : cards) {
            res = res * 100 + (long) card;
        }
        return res;
    }

    /**
     * Returns a list of five card values that form a straight if available.
     * The method collects unique card values, appends a low Ace if applicable,
     * and then attempts to find five consecutive values.
     */
    public static List<Integer> getStraight(List<Card> cards) {
        if (cards.isEmpty()) {
            return new ArrayList<>();
        }
        // Collect unique values
        Set<Integer> uniqueSet = new HashSet<>();
        for (Card card : cards) {
            uniqueSet.add(card.getValue().getValue());
        }
        List<Integer> uniqueValues = new ArrayList<>(uniqueSet);
        uniqueValues.sort(Collections.reverseOrder());

        // If Ace is the highest card, consider Ace as low by adding 1 (if not already present)
        if (!uniqueValues.isEmpty() && uniqueValues.get(0) == 14 && !uniqueValues.contains(1)) {
            uniqueValues.add(1);
        }

        List<Integer> straight = new ArrayList<>();
        straight.add(uniqueValues.get(0));

        for (int i = 0; i < uniqueValues.size() - 1; i++) {
            // If current card is not exactly one greater than the next, reset the straight sequence.
            if (uniqueValues.get(i) != uniqueValues.get(i + 1) + 1) {
                straight.clear();
            }
            straight.add(uniqueValues.get(i + 1));
            if (straight.size() == 5) {
                break;
            }
        }
        // Sort the resulting straight in descending order before returning.
        straight.sort(Collections.reverseOrder());
        return straight;
    }

    public static String cardsToRankString(List<Card> cards) {
        long rankNumber = cardsToRankNumber(cards);
        int typeNumber = (int) (rankNumber / Math.pow(100,5));
        return switch (typeNumber) {
            //StraightFlush
            case 9 -> {
                int kicker = (int) (rankNumber / Math.pow(100,4)) % 100;
                if (kicker == 14) yield "royal flush";
                else yield CardHelper.numberToWord(kicker) + "-high straight flush";
            }
            //FourOfAKind
            case 8 -> "four " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,4)) % 100);
            //FullHouse
            case 7 -> "full house " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,4)) % 100) + " full of " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,1)) % 100);
            //Flush
            case 6 -> CardHelper.numberToWord((int) (rankNumber / Math.pow(100,4)) % 100) + "-high flush";
            //Straight
            case 5 -> CardHelper.numberToWord((int) (rankNumber / Math.pow(100,4)) % 100) + "-high straight";
            //ThreeOfAKind
            case 4 -> "three " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,4)) % 100);
            //TwoPair
            case 3 -> "two pair: " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,4)) % 100) + " over " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,2)) % 100);
            //Pair
            case 2 -> "pair of " + CardHelper.numberToPluralWord((int) (rankNumber / Math.pow(100,4)) % 100);
            //HighCard
            case 1 -> CardHelper.numberToWord((int) (rankNumber / Math.pow(100,4)) % 100) + " high";
            //Todo: Missing error Handling
            //Todo: Import CardHelperClass
            default -> "null";
        };
    }

    private static Suit findPrimarySuite(List<Card> cards) {
        int[] suitCounter = new int[4];
        for (Card card : cards) {
            switch (card.getSuit()) {
                case SPADES: suitCounter[0]++;
                    break;
                case HEARTS: suitCounter[1]++;
                    break;
                case CLUBS: suitCounter[2]++;
                    break;
                case DIAMONDS: suitCounter[3]++;
                    break;
                default: 
                    //Todo: missing error handling
                    break;
            }
        }

        if (suitCounter[0] >= suitCounter[1] && suitCounter[0] >= suitCounter[2] && suitCounter[0] >= suitCounter[3]) {
            return Suit.SPADES;
        } else if (suitCounter[1] >= suitCounter[0] && suitCounter[1] >= suitCounter[2] && suitCounter[1] >= suitCounter[3]) {
            return Suit.HEARTS;
        } else if (suitCounter[2] >= suitCounter[0] && suitCounter[2] >= suitCounter[1] && suitCounter[2] >= suitCounter[3]) {
            return Suit.CLUBS;
        } else {
            return Suit.DIAMONDS;
        }
    }
}
