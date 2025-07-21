package com.spadeboot.domain.card;

public class CardHelper {
    public static String numberToWord(int cardNumber) {
        return switch (cardNumber) {
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 8 -> "eight";
            case 9 -> "nine";
            case 10 -> "ten";
            case 11 -> "jack";
            case 12 -> "queen";
            case 13 -> "king";
            case 14 -> "ace";
            //Todo: Missing error Handling
            default -> "null";
        };
    }

    public static String numberToPluralWord(int cardNumber) {
        return switch (cardNumber) {
            case 2 -> "twos";
            case 3 -> "threes";
            case 4 -> "fours";
            case 5 -> "fives";
            case 6 -> "sixes";
            case 7 -> "sevens";
            case 8 -> "eights";
            case 9 -> "nines";
            case 10 -> "tens";
            case 11 -> "jacks";
            case 12 -> "queens";
            case 13 -> "kings";
            case 14 -> "aces";
            //Todo: Missing error Handling
            default -> "null";
        };
    }
}
