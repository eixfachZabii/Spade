// src/main/java/com/spadeboot/websocket/dto/PlayerActionResponse.java
package com.spadeboot.api.dto;

import lombok.Data;

@Data
public class PlayerActionResponse {
    private boolean success;
    private String message;
    private PlayerActionDto action;
    private int newPot;
    private int playerChipsRemaining;
    private boolean isStateChanged; // True if action completed a stage/round
    private Long nextPlayerId;
}
