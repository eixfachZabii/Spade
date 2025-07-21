
// src/main/java/com/pokerapp/websocket/dto/GameEventDto.java
package com.spadeboot.api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GameEventDto {
    public enum EventType {
        GAME_STARTED,
        GAME_ENDED,
        ROUND_STARTED,
        ROUND_ENDED,
        STAGE_CHANGED,
        PLAYER_ACTION,
        PLAYER_TURN,
        PLAYER_CONNECTED,
        PLAYER_DISCONNECTED,
        CARDS_DEALT,
        COMMUNITY_CARDS_REVEALED,
        WINNER_DECLARED,
        POT_DISTRIBUTED,
        ERROR
    }

    private EventType type;
    private LocalDateTime timestamp = LocalDateTime.now();
    private Object payload;
    private String message;
}
