// src/main/java/com/pokerapp/api/dto/PlayerStateDto.java
package com.spadeboot.api.dto;

import com.spadeboot.domain.user.PlayerStatus;
import lombok.Data;

import java.util.List;

@Data
public class PlayerStateDto {
    private Long playerId;
    private String username;
    private int chips;
    private int currentBet;
    private PlayerStatus status;
    private boolean isConnected;
    private boolean hasCards;
    private Integer seatPosition;
    private boolean isDealer;
    private boolean isSmallBlind;
    private boolean isBigBlind;
    private boolean isPlayerTurn;

    // Add hole cards for debugging purposes
    private List<String> holeCards;
}