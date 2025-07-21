// src/main/java/com/pokerapp/session/RoundSession.java
package com.spadeboot.session;

import com.spadeboot.domain.card.Card;
import com.spadeboot.domain.card.Deck;
import com.spadeboot.domain.game.*;
import com.spadeboot.domain.user.Player;
import com.spadeboot.domain.user.PlayerStatus;
import com.spadeboot.exception.InvalidMoveException;
import com.spadeboot.api.dto.PlayerActionDto;
import com.spadeboot.api.dto.PlayerActionResponse;
import lombok.Getter;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RoundSession extends Thread {

    private final GameSession gameSession;
    private final Game game;
    private final List<Player> players;
    private final Map<Integer, Player> seatPositions;
    private final int smallBlindPos;
    private final int bigBlindPos;
    private final int smallBlind;
    private final int bigBlind;

    @Getter
    private Round round;
    private Deck deck;
    private Map<Long, Pair<Card, Card>> playerHands;
    private List<Card> communityCards;

    // Current state
    @Getter
    private StageType currentStage;
    @Getter
    private int pot;
    @Getter
    private int currentBet;
    @Getter
    private Long currentPlayerTurn;
    private int currentPlayerIndex;

    // Betting tracking - Fixed to properly handle betting rounds
    private Map<Long, Integer> playerBetsThisRound; // Current betting round bets
    private Map<Long, Integer> totalPlayerBets; // Total bets in pot across all rounds
    private Map<Long, PlayerStatus> playerStatuses;

    // Action handling
    private CountDownLatch actionLatch;
    private PlayerActionDto pendingAction;
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    // Timeout settings
    private static final long ACTION_TIMEOUT_SECONDS = 600;

    public RoundSession(GameSession gameSession, Game game, List<Player> players,
                        Map<Integer, Player> seatPositions, int smallBlindPos,
                        int bigBlindPos, int smallBlind, int bigBlind) {
        this.gameSession = gameSession;
        this.game = game;
        this.players = new ArrayList<>(players);
        this.seatPositions = seatPositions;
        this.smallBlindPos = smallBlindPos;
        this.bigBlindPos = bigBlindPos;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;

        initializeRound();
    }

    private void initializeRound() {
        this.round = new Round();
        this.round.setGame(game);
        this.round.setPlayers(new ArrayList<>(players));
        this.round.setPlayerCount(players.size());

        this.deck = new Deck();
        this.deck.initialize();

        this.playerHands = new HashMap<>();
        this.communityCards = new ArrayList<>();
        this.playerBetsThisRound = new ConcurrentHashMap<>();
        this.totalPlayerBets = new ConcurrentHashMap<>();
        this.playerStatuses = new ConcurrentHashMap<>();

        this.pot = 0;
        this.currentBet = 0;

        // Initialize player statuses and betting maps
        for (Player player : players) {
            // Reset all players to ACTIVE status for this round (fixes fold status reset issue)
            playerStatuses.put(player.getId(), PlayerStatus.ACTIVE);
            player.setStatus(PlayerStatus.ACTIVE);

            playerBetsThisRound.put(player.getId(), 0);
            totalPlayerBets.put(player.getId(), 0);
        }
    }

    @Override
    public void run() {
        try {
            // Deal cards to players
            dealPlayerCards();

            // Post blinds
            postBlinds();

            // Play each stage
            playStage(StageType.PRE_FLOP);
            if (shouldContinue()) {
                dealCommunityCards(3); // Flop
                playStage(StageType.FLOP);
            }
            if (shouldContinue()) {
                dealCommunityCards(1); // Turn
                playStage(StageType.TURN);
            }
            if (shouldContinue()) {
                dealCommunityCards(1); // River
                playStage(StageType.RIVER);
            }

            // Determine winner and distribute pot
            determineWinnerAndDistributePot();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dealPlayerCards() {
        for (Player player : players) {
            if (player.getChips() > 0) {
                Card card1 = deck.drawCard();
                Card card2 = deck.drawCard();
                playerHands.put(player.getId(), Pair.of(card1, card2));
            }
        }
        round.setPlayerHands(playerHands);
    }

    private void postBlinds() {
        Player smallBlindPlayer = players.get(smallBlindPos);
        Player bigBlindPlayer = players.get(bigBlindPos);

        // Post small blind
        int smallBlindAmount = Math.min(smallBlind, smallBlindPlayer.getChips());
        placeBet(smallBlindPlayer.getId(), smallBlindAmount);

        // Post big blind
        int bigBlindAmount = Math.min(bigBlind, bigBlindPlayer.getChips());
        placeBet(bigBlindPlayer.getId(), bigBlindAmount);

        // Set current bet to big blind amount
        currentBet = bigBlindAmount;

        System.out.println("Blinds posted - Small: " + smallBlindAmount + ", Big: " + bigBlindAmount + ", Current bet: " + currentBet);
    }

    private void playStage(StageType stage) {
        currentStage = stage;
        Stage stageEntity = new Stage();
        stageEntity.setType(stage);
        stageEntity.setRound(round);

        // FIXED: Only reset bets for non-preflop stages
        // For preflop, blinds are already posted and should count toward bets
        if (stage != StageType.PRE_FLOP) {
            // Reset player bets for new betting round (post-flop stages)
            playerBetsThisRound.clear();
            for (Long playerId : playerStatuses.keySet()) {
                if (playerStatuses.get(playerId) != PlayerStatus.FOLDED) {
                    playerBetsThisRound.put(playerId, 0);
                }
            }
            currentBet = 0; // Reset current bet for post-flop betting
        }

        // Set starting player
        if (stage == StageType.PRE_FLOP) {
            // Pre-flop starts after big blind
            currentPlayerIndex = getNextActivePlayerIndex(bigBlindPos);
        } else {
            // Other stages start from small blind or first active player
            currentPlayerIndex = findFirstActivePlayerFromPosition(smallBlindPos);
        }

        // Betting round logic
        conductBettingRound();

        // Save stage to round
        switch (stage) {
            case PRE_FLOP -> round.setPreFlop(stageEntity);
            case FLOP -> round.setFlop(stageEntity);
            case TURN -> round.setTurn(stageEntity);
            case RIVER -> round.setRiver(stageEntity);
        }
    }

    private void conductBettingRound() {
        boolean bettingComplete = false;
        int lastRaiserIndex = -1;
        int playersActed = 0;
        int totalActivePlayers = getActivePlayerCount();

        // Track which players have had a chance to act this betting round
        Set<Long> playersWhoActed = new HashSet<>();

        while (!bettingComplete && !shouldStop.get() && totalActivePlayers > 1) {
            Player currentPlayer = players.get(currentPlayerIndex);

            if (isPlayerActive(currentPlayer.getId())) {
                currentPlayerTurn = currentPlayer.getId();

                // Check if player needs to act
                int playerCurrentBet = playerBetsThisRound.getOrDefault(currentPlayer.getId(), 0);
                boolean needsToAct = playerCurrentBet < currentBet || !playersWhoActed.contains(currentPlayer.getId());

                if (needsToAct && currentPlayer.getChips() > 0) {
                    // Wait for player action
                    PlayerActionDto action = waitForPlayerAction(currentPlayer);

                    if (action != null) {
                        // Process the action
                        processActionInternal(currentPlayer, action);
                        playersWhoActed.add(currentPlayer.getId());

                        // Check if this was a raise
                        if (action.getAction() == PlayerActionDto.ActionType.RAISE) {
                            lastRaiserIndex = currentPlayerIndex;
                            playersWhoActed.clear(); // Reset - everyone needs to act again
                            playersWhoActed.add(currentPlayer.getId());
                        }
                    }
                } else {
                    // Player doesn't need to act (already matched current bet)
                    playersWhoActed.add(currentPlayer.getId());
                }
            }

            // Move to next player
            int nextIndex = getNextActivePlayerIndex(currentPlayerIndex);

            // Check if betting round is complete
            if (nextIndex == currentPlayerIndex || // Wrapped around to same player
                    getActivePlayerCount() <= 1 || // Only one player left
                    (playersWhoActed.size() >= getActivePlayerCount() && allPlayersMatchedBet())) {
                bettingComplete = true;
            } else {
                currentPlayerIndex = nextIndex;
            }
        }
    }

    private boolean allPlayersMatchedBet() {
        for (Player player : players) {
            if (isPlayerActive(player.getId()) && player.getChips() > 0) {
                int playerBet = playerBetsThisRound.getOrDefault(player.getId(), 0);
                if (playerBet < currentBet) {
                    return false;
                }
            }
        }
        return true;
    }

    private void dealCommunityCards(int count) {
        List<Card> newCards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card card = deck.drawCard();
            card.setShowing(true);
            communityCards.add(card);
            newCards.add(card);
        }

        System.out.println("Dealt " + count + " community cards for " + currentStage);
    }

    /**
     * Process a player action (called from GameSession)
     */
    public synchronized PlayerActionResponse processPlayerAction(Player player, PlayerActionDto action) {
        if (!isPlayerTurn(player.getId())) {
            throw new InvalidMoveException("It's not your turn");
        }

        // Validate action
        validateAction(player, action);

        // Set the pending action
        this.pendingAction = action;

        // Release the latch to continue the round
        if (actionLatch != null) {
            actionLatch.countDown();
        }

        // Create response
        PlayerActionResponse response = new PlayerActionResponse();
        response.setSuccess(true);
        response.setAction(action);
        response.setNewPot(pot);
        response.setPlayerChipsRemaining(player.getChips());

        // Find next active player
        int nextPlayerIndex = getNextActivePlayerIndex(currentPlayerIndex);
        if (nextPlayerIndex != currentPlayerIndex) {
            response.setNextPlayerId(players.get(nextPlayerIndex).getId());
        }

        return response;
    }

    private void processActionInternal(Player player, PlayerActionDto action) {
        Long playerId = player.getId();

        switch (action.getAction()) {
            case FOLD -> handleFold(player);
            case CHECK -> handleCheck(player);
            case CALL -> handleCall(player);
            case RAISE -> handleRaise(player, action.getAmount());
            case ALL_IN -> handleAllIn(player);
        }

        System.out.println("Player " + player.getUser().getUsername() + " action: " + action.getAction() +
                ", Current bet: " + currentBet + ", Player bet this round: " + playerBetsThisRound.get(playerId));
    }

    private void handleFold(Player player) {
        playerStatuses.put(player.getId(), PlayerStatus.FOLDED);
        player.setStatus(PlayerStatus.FOLDED);
        System.out.println("Player " + player.getUser().getUsername() + " folded");
    }

    private void handleCheck(Player player) {
        int playerCurrentBet = playerBetsThisRound.getOrDefault(player.getId(), 0);
        if (currentBet > playerCurrentBet) {
            throw new InvalidMoveException("Cannot check, must call " + (currentBet - playerCurrentBet) + " or fold");
        }
        System.out.println("Player " + player.getUser().getUsername() + " checked");
    }

    private void handleCall(Player player) {
        Long playerId = player.getId();
        int playerCurrentBet = playerBetsThisRound.getOrDefault(playerId, 0);
        int toCall = currentBet - playerCurrentBet;

        if (toCall <= 0) {
            throw new InvalidMoveException("Nothing to call");
        }

        int actualBet = Math.min(toCall, player.getChips());
        placeBet(playerId, actualBet);

        System.out.println("Player " + player.getUser().getUsername() + " called " + actualBet +
                " (needed to call " + toCall + ")");
    }

    private void handleRaise(Player player, int raiseAmount) {
        Long playerId = player.getId();
        int playerCurrentBet = playerBetsThisRound.getOrDefault(playerId, 0);
        int toCall = currentBet - playerCurrentBet;
        int totalBetIncrease = toCall + raiseAmount;

        if (totalBetIncrease > player.getChips()) {
            throw new InvalidMoveException("Insufficient chips for raise");
        }

        if (raiseAmount < bigBlind) {
            throw new InvalidMoveException("Minimum raise is " + bigBlind);
        }

        placeBet(playerId, totalBetIncrease);
        currentBet = playerBetsThisRound.get(playerId);

        System.out.println("Player " + player.getUser().getUsername() + " raised by " + raiseAmount +
                ", new current bet: " + currentBet);
    }

    private void handleAllIn(Player player) {
        Long playerId = player.getId();
        int allInAmount = player.getChips();
        int playerCurrentBet = playerBetsThisRound.getOrDefault(playerId, 0);

        placeBet(playerId, allInAmount);

        // Update current bet if this all-in is higher
        if (playerBetsThisRound.get(playerId) > currentBet) {
            currentBet = playerBetsThisRound.get(playerId);
        }

        System.out.println("Player " + player.getUser().getUsername() + " went all-in with " + allInAmount);
    }

    private void placeBet(Long playerId, int amount) {
        Player player = findPlayerById(playerId);

        // Deduct from player chips
        player.setChips(player.getChips() - amount);

        // Add to current round bets
        playerBetsThisRound.put(playerId, playerBetsThisRound.getOrDefault(playerId, 0) + amount);

        // Add to total bets
        totalPlayerBets.put(playerId, totalPlayerBets.getOrDefault(playerId, 0) + amount);

        // Add to pot
        pot += amount;
    }

    private void validateAction(Player player, PlayerActionDto action) {
        Long playerId = player.getId();
        int playerCurrentBet = playerBetsThisRound.getOrDefault(playerId, 0);

        switch (action.getAction()) {
            case CHECK -> {
                if (currentBet > playerCurrentBet) {
                    throw new InvalidMoveException("Cannot check when there's a bet to call (" +
                            (currentBet - playerCurrentBet) + " to call)");
                }
            }
            case CALL -> {
                if (currentBet <= playerCurrentBet) {
                    throw new InvalidMoveException("Nothing to call");
                }
            }
            case RAISE -> {
                if (action.getAmount() == null || action.getAmount() <= 0) {
                    throw new InvalidMoveException("Raise amount must be positive");
                }
                if (action.getAmount() < bigBlind) {
                    throw new InvalidMoveException("Minimum raise is " + bigBlind);
                }

                int toCall = currentBet - playerCurrentBet;
                int totalNeeded = toCall + action.getAmount();
                if (totalNeeded > player.getChips()) {
                    throw new InvalidMoveException("Insufficient chips for raise");
                }
            }
        }
    }

    private PlayerActionDto waitForPlayerAction(Player player) {
        actionLatch = new CountDownLatch(1);
        pendingAction = null;

        try {
            // Wait for action with timeout
            boolean received = actionLatch.await(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!received) {
                // Auto-fold on timeout
                PlayerActionDto timeoutAction = new PlayerActionDto();
                timeoutAction.setAction(PlayerActionDto.ActionType.FOLD);
                System.out.println("Player " + player.getUser().getUsername() + " timed out, auto-folding");
                return timeoutAction;
            }

            return pendingAction;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void determineWinnerAndDistributePot() {
        List<Player> activePlayers = players.stream()
                .filter(p -> playerStatuses.get(p.getId()) == PlayerStatus.ACTIVE)
                .toList();

        if (activePlayers.size() == 1) {
            // Only one player left, they win
            Player winner = activePlayers.get(0);
            winner.setChips(winner.getChips() + pot);
            round.setWinner(List.of(winner));
            System.out.println("Player " + winner.getUser().getUsername() + " wins " + pot + " chips (only player left)");
        } else {
            // Evaluate hands
            Map<Player, Long> handRanks = new HashMap<>();

            for (Player player : activePlayers) {
                List<Card> playerCards = new ArrayList<>();
                Pair<Card, Card> hand = playerHands.get(player.getId());
                playerCards.add(hand.getFirst());
                playerCards.add(hand.getSecond());
                playerCards.addAll(communityCards);

                long rank = HandEvaluation.cardsToRankNumber(playerCards);
                handRanks.put(player, rank);
            }

            // Find winner(s)
            long bestRank = handRanks.values().stream().max(Long::compare).orElse(0L);
            List<Player> winners = handRanks.entrySet().stream()
                    .filter(e -> e.getValue() == bestRank)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // Split pot among winners
            int winnerShare = pot / winners.size();
            for (Player winner : winners) {
                winner.setChips(winner.getChips() + winnerShare);
                System.out.println("Player " + winner.getUser().getUsername() + " wins " + winnerShare + " chips");
            }

            round.setWinner(winners);
        }
    }

    // Helper methods

    public boolean isPlayerTurn(Long playerId) {
        return playerId.equals(currentPlayerTurn);
    }

    public int getPlayerCurrentBet(Long playerId) {
        return playerBetsThisRound.getOrDefault(playerId, 0);
    }

    public boolean playerHasCards(Long playerId) {
        return playerHands.containsKey(playerId);
    }

    public List<String> getCommunityCardsAsStrings() {
        return communityCards.stream()
                .map(card -> card.getValue().name() + card.getSuit().name().charAt(0))
                .collect(Collectors.toList());
    }

    public List<String> getPlayerHoleCardsAsStrings(Long playerId) {
        Pair<Card, Card> hand = playerHands.get(playerId);
        if (hand == null) {
            return new ArrayList<>();
        }

        List<String> cards = new ArrayList<>();
        cards.add(hand.getFirst().getValue().name() + hand.getFirst().getSuit().name().charAt(0));
        cards.add(hand.getSecond().getValue().name() + hand.getSecond().getSuit().name().charAt(0));
        return cards;
    }

    private boolean shouldContinue() {
        return getActivePlayerCount() > 1 && !shouldStop.get();
    }

    private int getActivePlayerCount() {
        return (int) playerStatuses.values().stream()
                .filter(status -> status == PlayerStatus.ACTIVE)
                .count();
    }

    private boolean isPlayerActive(Long playerId) {
        return playerStatuses.get(playerId) == PlayerStatus.ACTIVE;
    }

    private int getNextActivePlayerIndex(int currentIndex) {
        int nextIndex = (currentIndex + 1) % players.size();
        int attempts = 0;

        while (!isPlayerActive(players.get(nextIndex).getId()) && attempts < players.size()) {
            nextIndex = (nextIndex + 1) % players.size();
            attempts++;
        }

        return nextIndex;
    }

    private Player getNextActivePlayer(int currentIndex) {
        return players.get(getNextActivePlayerIndex(currentIndex));
    }

    private int findFirstActivePlayerFromPosition(int startPos) {
        int index = startPos;
        int attempts = 0;

        while (!isPlayerActive(players.get(index).getId()) && attempts < players.size()) {
            index = (index + 1) % players.size();
            attempts++;
        }

        return index;
    }

    private Player findPlayerById(Long playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player not found: " + playerId));
    }

    public void endRound() {
        shouldStop.set(true);
        if (actionLatch != null) {
            actionLatch.countDown();
        }
    }
}