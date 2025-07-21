
// src/main/java/com/pokerapp/websocket/GameEventPublisher.java
package com.spadeboot.websocket;

import com.spadeboot.api.dto.GameEventDto;
import com.spadeboot.api.dto.PlayerActionDto;
import com.spadeboot.domain.game.StageType;
import com.spadeboot.api.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameEventPublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_PREFIX = "/topic/tables/";
    private static final String USER_QUEUE_PREFIX = "/queue/";

    /**
     * Publish game started event
     */
    public void publishGameStarted(Long tableId, GameStateDto gameState) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.GAME_STARTED);
        event.setPayload(gameState);
        event.setMessage("Game has started!");

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish game ended event
     */
    public void publishGameEnded(Long tableId) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.GAME_ENDED);
        event.setMessage("Game has ended");

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish player action event
     */
    public void publishPlayerAction(Long tableId, Long playerId, PlayerActionDto action, PlayerActionResponse response) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.PLAYER_ACTION);
        event.setPayload(response);
        event.setMessage(String.format("Player %d: %s", playerId, action.getAction()));

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish game state update
     */
    public void publishGameStateUpdate(Long tableId, GameStateDto gameState) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.STAGE_CHANGED);
        event.setPayload(gameState);

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish player turn event
     */
    public void publishPlayerTurn(Long tableId, Long playerId) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.PLAYER_TURN);
        event.setPayload(playerId);
        event.setMessage("Your turn!");

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish community cards revealed
     */
    public void publishCommunityCards(Long tableId, StageType stage, List<String> cards) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.COMMUNITY_CARDS_REVEALED);
        event.setPayload(cards);
        event.setMessage(stage.name() + " cards revealed");

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish winner declared
     */
    public void publishWinner(Long tableId, WinnerDto winner) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.WINNER_DECLARED);
        event.setPayload(winner);
        event.setMessage(winner.getUsername() + " wins with " + winner.getHandRank());

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish player disconnected
     */
    public void publishPlayerDisconnected(Long tableId, Long playerId) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.PLAYER_DISCONNECTED);
        event.setPayload(playerId);

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Publish player reconnected
     */
    public void publishPlayerReconnected(Long tableId, Long playerId) {
        GameEventDto event = new GameEventDto();
        event.setType(GameEventDto.EventType.PLAYER_CONNECTED);
        event.setPayload(playerId);

        messagingTemplate.convertAndSend(TOPIC_PREFIX + tableId, event);
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(String sessionId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
    }
}