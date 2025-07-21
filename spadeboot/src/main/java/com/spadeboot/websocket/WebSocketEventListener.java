// src/main/java/com/pokerapp/websocket/WebSocketEventListener.java
package com.spadeboot.websocket;

import com.spadeboot.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @Autowired
    private GameService gameService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Log connection
        System.out.println("WebSocket connection established: " + sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Get user and table info from session attributes
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        Long tableId = (Long) headerAccessor.getSessionAttributes().get("tableId");

        if (userId != null && tableId != null) {
            // Handle player disconnection
            gameService.handlePlayerDisconnect(tableId, userId);
        }

        // Log disconnection
        System.out.println("WebSocket disconnection: " + sessionId);
    }
}