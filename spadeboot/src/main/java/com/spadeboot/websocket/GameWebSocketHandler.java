// src/main/java/com/pokerapp/websocket/GameWebSocketHandler.java
package com.spadeboot.websocket;

import com.spadeboot.service.GameService;
import com.spadeboot.api.dto.PlayerActionDto;
import com.spadeboot.api.dto.GameEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketHandler {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameEventPublisher eventPublisher;

    /**
     * Handle player actions for a specific table
     */
    @MessageMapping("/game/{tableId}/action")
    @SendToUser("/queue/errors")
    public void handlePlayerAction(
            @DestinationVariable Long tableId,
            @Payload PlayerActionDto action,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                throw new IllegalStateException("User not authenticated");
            }

            Long userId = getUserIdFromAuth(auth);
            gameService.processPlayerAction(tableId, userId, action);

        } catch (Exception e) {
            // Send error back to the user
            GameEventDto errorEvent = new GameEventDto();
            errorEvent.setType(GameEventDto.EventType.ERROR);
            errorEvent.setMessage(e.getMessage());
            eventPublisher.sendToUser(headerAccessor.getSessionId(), "/queue/errors", errorEvent);
        }
    }

    /**
     * Handle player connection to a game
     */
    @MessageMapping("/game/{tableId}/connect")
    public void handleConnect(
            @DestinationVariable Long tableId,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            Long userId = getUserIdFromAuth(auth);

            // Subscribe the user to the table's topic
            headerAccessor.getSessionAttributes().put("tableId", tableId);
            headerAccessor.getSessionAttributes().put("userId", userId);

            // Handle reconnection logic
            try {
                gameService.handlePlayerReconnect(tableId, userId);
            } catch (Exception e) {
                // Player might be connecting for the first time, not a reconnection
            }
        }
    }

    /**
     * Handle player disconnection
     */
    @MessageMapping("/game/{tableId}/disconnect")
    public void handleDisconnect(
            @DestinationVariable Long tableId,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            Long userId = getUserIdFromAuth(auth);
            gameService.handlePlayerDisconnect(tableId, userId);
        }
    }

    /**
     * Extract user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication auth) {
        // This depends on your security implementation
        // Assuming UserDetailsImpl has getId() method
        return ((com.spadeboot.security.UserDetailsImpl) auth.getPrincipal()).getId();
    }
}
