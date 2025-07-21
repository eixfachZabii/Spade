// src/main/java/com/pokerapp/session/SessionManager.java
package com.spadeboot.session;

import com.spadeboot.domain.user.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final Map<Long, GameSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Create a new game session for a table
     */
    public GameSession createGameSession(Long tableId, List<Player> players, int bigBlind) {
        if (activeSessions.containsKey(tableId)) {
            throw new IllegalStateException("Game session already exists for table " + tableId);
        }

        GameSession session = new GameSession(tableId, players, bigBlind);
        activeSessions.put(tableId, session);
        return session;
    }

    /**
     * Get an active game session
     */
    public GameSession getGameSession(Long tableId) {
        return activeSessions.get(tableId);
    }

    /**
     * Remove a game session
     */
    public void removeGameSession(Long tableId) {
        GameSession session = activeSessions.remove(tableId);
        if (session != null && session.isAlive()) {
            session.endGame();
        }
    }

    /**
     * Check if a table has an active game
     */
    public boolean hasActiveGame(Long tableId) {
        GameSession session = activeSessions.get(tableId);
        return session != null && session.isGameActive();
    }

    /**
     * Get all active table IDs
     */
    public List<Long> getActiveTableIds() {
        return activeSessions.keySet().stream().toList();
    }

    /**
     * Cleanup inactive sessions
     */
    public void cleanupInactiveSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            GameSession session = entry.getValue();
            return !session.isAlive() || !session.isGameActive();
        });
    }
}