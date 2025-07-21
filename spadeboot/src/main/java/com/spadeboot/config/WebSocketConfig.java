// src/main/java/com/pokerapp/config/WebSocketConfig.java
package com.spadeboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Set prefix for messages FROM the server TO the client
        registry.enableSimpleBroker("/topic", "/queue");

        // Set prefix for messages FROM the client TO the server
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint with proper CORS configuration
        registry
                .addEndpoint("/ws")
                .setAllowedOrigins(
                        "https://localhost:3000",
                        "http://localhost:3000",
                        "https://127.0.0.1:3000",
                        "http://127.0.0.1:3000"
                )
                .withSockJS();

        // Also register without SockJS for direct WebSocket connections
        registry
                .addEndpoint("/ws")
                .setAllowedOrigins(
                        "https://localhost:3000",
                        "http://localhost:3000",
                        "https://127.0.0.1:3000",
                        "http://127.0.0.1:3000"
                );
    }
}