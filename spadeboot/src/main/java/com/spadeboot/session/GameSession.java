// src/main/java/com/pokerapp/session/GameSession.java
package com.spadeboot.session;

import com.spadeboot.domain.game.Game;
import com.spadeboot.domain.user.Player;
import com.spadeboot.domain.user.PlayerStatus;
import com.spadeboot.exception.InvalidMoveException;
import com.spadeboot.api.dto.*;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class GameSession extends Thread {

    @Getter
    private final Long tableId;
    private final int bigBlind;
    private final int smallBlind;

    @Getter
    private Game game;
    private List<Player> players;
    private Map<Integer, Player> seatPositions;
    private Map<Long, PlayerInfo> playerInfoMap;

    private RoundSession currentRound;
    private final AtomicBoolean gameActive = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private final ReentrantLock actionLock = new ReentrantLock();

    private int dealerPosition = 0;
    private int roundNumber = 0;

    // Player connection tracking
    private final Map<Long, Boolean> playerConnections = new ConcurrentHashMap<>();

    // Action queue for processing player actions
    private PlayerActionDto pendingAction;
    private Long pendingActionPlayerId;

    public GameSession(Long tableId, List<Player> players, int bigBlind) {
        this.tableId = tableId;
        this.players = new ArrayList<>(players);
        this.bigBlind = bigBlind;
        this.smallBlind = bigBlind / 2;

        initializeGame();
    }

    private void initializeGame() {
        this.game = new Game();
        this.game.setAllPlayers(new ArrayList<>(players));
        this.game.setCurrentPlayers(new ArrayList<>(players));

        // Initialize seat positions
        this.seatPositions = new HashMap<>();
        this.playerInfoMap = new HashMap<>();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            seatPositions.put(i, player);
            playerConnections.put(player.getId(), true);

            // FIXED: Reset all players to ACTIVE status at game start
            player.setStatus(PlayerStatus.ACTIVE);

            PlayerInfo info = new PlayerInfo();
            info.playerId = player.getId();
            info.seatPosition = i;
            info.isActive = true;
            playerInfoMap.put(player.getId(), info);
        }

        // Randomly select dealer position
        dealerPosition = new Random().nextInt(players.size());

        System.out.println("Game initialized with " + players.size() + " players, dealer at position " + dealerPosition);
    }

    @Override
    public void run() {
        gameActive.set(true);

        while (!shouldStop.get() && hasEnoughActivePlayers()) {
            try {
                // FIXED: Reset player statuses before each round
                resetPlayerStatusesForNewRound();

                // Start a new round
                startNewRound();

                // Wait for round to complete
                if (currentRound != null) {
                    currentRound.join();
                }

                // Move dealer button
                moveDealerButton();

                // Small delay between rounds
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        gameActive.set(false);
        System.out.println("Game session ended for table " + tableId);
    }

    /**
     * FIXED: Reset player statuses for new round
     * All players who are not SITTING_OUT should be set to ACTIVE
     * This ensures folded players can participate in the next hand
     */
    private void resetPlayerStatusesForNewRound() {
        for (Player player : players) {
            // Only reset players who have chips and are not sitting out
            if (player.getChips() > 0 && player.getStatus() != PlayerStatus.SITTING_OUT) {
                player.setStatus(PlayerStatus.ACTIVE);

                // Also ensure player info is active
                PlayerInfo info = playerInfoMap.get(player.getId());
                if (info != null) {
                    info.isActive = true;
                }
            } else if (player.getChips() <= 0) {
                // Players with no chips are out of the game
                player.setStatus(PlayerStatus.SITTING_OUT);

                PlayerInfo info = playerInfoMap.get(player.getId());
                if (info != null) {
                    info.isActive = false;
                }
            }
        }

        System.out.println("Reset player statuses for new round. Active players: " + getActivePlayers().size());
    }

    private void startNewRound() {
        roundNumber++;

        // Get active players for this round
        List<Player> activePlayers = getActivePlayers();

        if (activePlayers.size() < 2) {
            System.out.println("Not enough active players (" + activePlayers.size() + ") to start round " + roundNumber);
            return;
        }

        System.out.println("Starting round " + roundNumber + " with " + activePlayers.size() + " players");

        // Calculate blind positions relative to active players
        int activePlayersCount = activePlayers.size();

        // Find dealer position among active players
        int activeDealerIndex = findActiveDealerIndex(activePlayers);

        // Calculate blind positions
        int smallBlindPos = (activeDealerIndex + 1) % activePlayersCount;
        int bigBlindPos = (activeDealerIndex + 2) % activePlayersCount;

        // For heads-up (2 players), dealer posts small blind
        if (activePlayersCount == 2) {
            smallBlindPos = activeDealerIndex;
            bigBlindPos = (activeDealerIndex + 1) % activePlayersCount;
        }

        System.out.println("Dealer: " + activePlayers.get(activeDealerIndex).getUser().getUsername() +
                ", Small Blind: " + activePlayers.get(smallBlindPos).getUser().getUsername() +
                ", Big Blind: " + activePlayers.get(bigBlindPos).getUser().getUsername());

        // Create and start round session
        currentRound = new RoundSession(
                this,
                game,
                activePlayers,
                createActiveSeatPositions(activePlayers),
                smallBlindPos,
                bigBlindPos,
                smallBlind,
                bigBlind
        );

        currentRound.start();
    }

    /**
     * Find the dealer index among active players
     */
    private int findActiveDealerIndex(List<Player> activePlayers) {
        // Find current dealer in the active players list
        Player currentDealer = seatPositions.get(dealerPosition);

        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getId().equals(currentDealer.getId())) {
                return i;
            }
        }

        // If current dealer is not active, use first active player
        return 0;
    }

    /**
     * Create seat positions map for active players only
     */
    private Map<Integer, Player> createActiveSeatPositions(List<Player> activePlayers) {
        Map<Integer, Player> activeSeats = new HashMap<>();
        for (int i = 0; i < activePlayers.size(); i++) {
            activeSeats.put(i, activePlayers.get(i));
        }
        return activeSeats;
    }

    /**
     * Process a player action
     */
    public synchronized PlayerActionResponse processAction(Player player, PlayerActionDto action) {
        actionLock.lock();
        try {
            if (currentRound == null || !currentRound.isAlive()) {
                throw new InvalidMoveException("No active round");
            }

            // Validate and process the action through the round session
            return currentRound.processPlayerAction(player, action);

        } finally {
            actionLock.unlock();
        }
    }

    /**
     * Check if it's a specific player's turn
     */
    public boolean isPlayerTurn(Long playerId) {
        if (currentRound == null) {
            return false;
        }
        return currentRound.isPlayerTurn(playerId);
    }

    /**
     * Get current game state
     */
    public GameStateDto getCurrentGameState() {
        GameStateDto state = new GameStateDto();
        state.setTableId(tableId);
        state.setGameId(game.getId());
        state.setRoundNumber(roundNumber);
        state.setGameActive(gameActive.get());

        if (currentRound != null && currentRound.isAlive()) {
            state.setCurrentStage(currentRound.getCurrentStage());
            state.setPot(currentRound.getPot());
            state.setCurrentBet(currentRound.getCurrentBet());
            state.setCurrentPlayerTurn(currentRound.getCurrentPlayerTurn());
            state.setCommunityCards(currentRound.getCommunityCardsAsStrings());
        }

        // Set player states
        List<PlayerStateDto> playerStates = new ArrayList<>();
        for (Map.Entry<Integer, Player> entry : seatPositions.entrySet()) {
            PlayerStateDto playerState = createPlayerStateDto(entry.getValue(), entry.getKey());
            playerStates.add(playerState);
        }
        state.setPlayers(playerStates);

        // Set positions
        state.setDealerPosition(getPlayerIdAtPosition(dealerPosition));

        // Calculate active blind positions
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.size() >= 2) {
            int activeDealerIndex = findActiveDealerIndex(activePlayers);
            int smallBlindIndex = (activeDealerIndex + 1) % activePlayers.size();
            int bigBlindIndex = (activeDealerIndex + 2) % activePlayers.size();

            // For heads-up
            if (activePlayers.size() == 2) {
                smallBlindIndex = activeDealerIndex;
                bigBlindIndex = (activeDealerIndex + 1) % activePlayers.size();
            }

            state.setSmallBlindPosition(activePlayers.get(smallBlindIndex).getId());
            state.setBigBlindPosition(activePlayers.get(bigBlindIndex).getId());
        }

        return state;
    }

    private PlayerStateDto createPlayerStateDto(Player player, int seatPosition) {
        PlayerStateDto dto = new PlayerStateDto();
        dto.setPlayerId(player.getId());
        dto.setUsername(player.getUser().getUsername());
        dto.setChips(player.getChips());
        dto.setStatus(player.getStatus());
        dto.setSeatPosition(seatPosition);
        dto.setConnected(playerConnections.getOrDefault(player.getId(), false));

        if (currentRound != null && currentRound.isAlive()) {
            dto.setCurrentBet(currentRound.getPlayerCurrentBet(player.getId()));
            dto.setHasCards(currentRound.playerHasCards(player.getId()));
            dto.setPlayerTurn(currentRound.isPlayerTurn(player.getId()));

            // Add hole cards for debugging purposes
            dto.setHoleCards(currentRound.getPlayerHoleCardsAsStrings(player.getId()));
        }

        dto.setDealer(seatPosition == dealerPosition);

        // Calculate blind positions for active players
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.size() >= 2) {
            int activeDealerIndex = findActiveDealerIndex(activePlayers);
            int smallBlindIndex = (activeDealerIndex + 1) % activePlayers.size();
            int bigBlindIndex = (activeDealerIndex + 2) % activePlayers.size();

            // For heads-up
            if (activePlayers.size() == 2) {
                smallBlindIndex = activeDealerIndex;
                bigBlindIndex = (activeDealerIndex + 1) % activePlayers.size();
            }

            dto.setSmallBlind(isPlayerAtActiveIndex(player, activePlayers, smallBlindIndex));
            dto.setBigBlind(isPlayerAtActiveIndex(player, activePlayers, bigBlindIndex));
        }

        return dto;
    }

    private boolean isPlayerAtActiveIndex(Player player, List<Player> activePlayers, int index) {
        return index < activePlayers.size() &&
                activePlayers.get(index).getId().equals(player.getId());
    }

    /**
     * Mark player as disconnected
     */
    public void markPlayerDisconnected(Long playerId) {
        playerConnections.put(playerId, false);
        PlayerInfo info = playerInfoMap.get(playerId);
        if (info != null) {
            info.isActive = false;
        }

        System.out.println("Player " + playerId + " disconnected");
    }

    /**
     * Mark player as reconnected
     */
    public void markPlayerReconnected(Long playerId) {
        playerConnections.put(playerId, true);
        PlayerInfo info = playerInfoMap.get(playerId);
        if (info != null && info.seatPosition != null) {
            info.isActive = true;
        }

        System.out.println("Player " + playerId + " reconnected");
    }

    /**
     * End the game
     */
    public void endGame() {
        shouldStop.set(true);
        if (currentRound != null) {
            currentRound.endRound();
        }
        this.interrupt();

        System.out.println("Game ended for table " + tableId);
    }

    /**
     * Check if game is active
     */
    public boolean isGameActive() {
        return gameActive.get() && !shouldStop.get();
    }

    // Helper methods

    private void moveDealerButton() {
        // Find next active player to be dealer
        int attempts = 0;
        do {
            dealerPosition = (dealerPosition + 1) % players.size();
            attempts++;
        } while (attempts < players.size() &&
                (seatPositions.get(dealerPosition).getChips() <= 0 ||
                        seatPositions.get(dealerPosition).getStatus() == PlayerStatus.SITTING_OUT));

        System.out.println("Dealer button moved to position " + dealerPosition +
                " (" + seatPositions.get(dealerPosition).getUser().getUsername() + ")");
    }

    private boolean hasEnoughActivePlayers() {
        return getActivePlayers().size() >= 2;
    }

    /**
     * FIXED: Get active players (not folded, not sitting out, have chips)
     */
    private List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> p.getChips() > 0)  // Must have chips
                .filter(p -> p.getStatus() != PlayerStatus.SITTING_OUT)  // Not sitting out
                .filter(p -> playerConnections.getOrDefault(p.getId(), true))  // Connected (or default to true)
                .collect(Collectors.toList());
    }

    private Long getPlayerIdAtPosition(int position) {
        Player player = seatPositions.get(position);
        return player != null ? player.getId() : null;
    }

    // Inner class to track player info
    private static class PlayerInfo {
        Long playerId;
        Integer seatPosition;
        boolean isActive;
    }
}