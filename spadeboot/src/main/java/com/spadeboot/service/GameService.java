// src/main/java/com/pokerapp/service/GameService.java
package com.spadeboot.service;

import com.spadeboot.domain.game.PokerTable;
import com.spadeboot.domain.user.Player;
import com.spadeboot.exception.InvalidMoveException;
import com.spadeboot.exception.NotFoundException;
import com.spadeboot.repository.PlayerRepository;
import com.spadeboot.repository.TableRepository;
import com.spadeboot.session.GameSession;
import com.spadeboot.session.SessionManager;
import com.spadeboot.websocket.GameEventPublisher;
import com.spadeboot.api.dto.GameStateDto;
import com.spadeboot.api.dto.PlayerActionDto;
import com.spadeboot.api.dto.PlayerActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private GameEventPublisher eventPublisher;

    /**
     * Start a new game at the specified table
     */
    @Transactional
    public GameStateDto startGame(Long tableId, Long userId, int bigBlind) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found"));

        // Verify the user is the table owner
        if (!table.getOwner().getUserId().equals(userId)) {
            throw new InvalidMoveException("Only the table owner can start the game");
        }

        // Check if there's already an active game
        if (sessionManager.hasActiveGame(tableId)) {
            throw new InvalidMoveException("Game is already in progress");
        }

        // Verify minimum players
        if (table.getPlayers().size() < 2) {
            throw new InvalidMoveException("Need at least 2 players to start the game");
        }

        // Create and start the game session
        GameSession gameSession = sessionManager.createGameSession(tableId, table.getPlayers().stream().toList(), bigBlind);
        gameSession.start();

        // Get initial game state
        GameStateDto gameState = gameSession.getCurrentGameState();

        // Broadcast game started event to all players
        eventPublisher.publishGameStarted(tableId, gameState);

        return gameState;
    }

    /**
     * End the current game at the specified table
     */
    @Transactional
    public void endGame(Long tableId, Long userId) {
        PokerTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new NotFoundException("Table not found"));

        // Verify the user is the table owner
        if (!table.getOwner().getUserId().equals(userId)) {
            throw new InvalidMoveException("Only the table owner can end the game");
        }

        GameSession session = sessionManager.getGameSession(tableId);
        if (session == null) {
            throw new InvalidMoveException("No active game found");
        }

        // End the game session
        session.endGame();
        sessionManager.removeGameSession(tableId);

        // Broadcast game ended event
        eventPublisher.publishGameEnded(tableId);
    }

    /**
     * Get the current game state for a table
     */
    public GameStateDto getGameState(Long tableId) {
        GameSession session = sessionManager.getGameSession(tableId);
        if (session == null) {
            throw new NotFoundException("No active game found for this table");
        }

        return session.getCurrentGameState();
    }

    /**
     * Process a player action
     */
    @Transactional
    public PlayerActionResponse processPlayerAction(Long tableId, Long userId, PlayerActionDto actionDto) {
        GameSession session = sessionManager.getGameSession(tableId);
        if (session == null) {
            throw new NotFoundException("No active game found for this table");
        }

        // Get the player
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Player not found"));

        // Validate it's the player's turn
        if (!session.isPlayerTurn(player.getId())) {
            throw new InvalidMoveException("It's not your turn");
        }

        // Process the action
        PlayerActionResponse response = session.processAction(player, actionDto);

        // Broadcast action to all players
        eventPublisher.publishPlayerAction(tableId, player.getId(), actionDto, response);

        // If the action completes a stage or round, broadcast the state update
        if (response.isStateChanged()) {
            GameStateDto newState = session.getCurrentGameState();
            eventPublisher.publishGameStateUpdate(tableId, newState);
        }

        return response;
    }

    /**
     * Handle player disconnection
     */
    public void handlePlayerDisconnect(Long tableId, Long userId) {
        GameSession session = sessionManager.getGameSession(tableId);
        if (session != null) {
            Player player = playerRepository.findByUserId(userId).orElse(null);
            if (player != null) {
                session.markPlayerDisconnected(player.getId());
                eventPublisher.publishPlayerDisconnected(tableId, player.getId());
            }
        }
    }

    /**
     * Handle player reconnection
     */
    public GameStateDto handlePlayerReconnect(Long tableId, Long userId) {
        GameSession session = sessionManager.getGameSession(tableId);
        if (session == null) {
            throw new NotFoundException("No active game found for this table");
        }

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Player not found"));

        session.markPlayerReconnected(player.getId());
        eventPublisher.publishPlayerReconnected(tableId, player.getId());

        return session.getCurrentGameState();
    }
}