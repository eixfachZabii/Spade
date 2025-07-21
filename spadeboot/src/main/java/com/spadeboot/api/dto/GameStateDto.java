// src/main/java/com/pokerapp/websocket/dto/GameStateDto.java
package com.spadeboot.api.dto;

import com.spadeboot.domain.game.StageType;
import lombok.Data;

import java.util.List;

@Data
public class GameStateDto {
    private Long tableId;
    private Long gameId;
    private int roundNumber;
    private StageType currentStage;
    private List<com.spadeboot.api.dto.PlayerStateDto> players;
    private List<String> communityCards;
    private int pot;
    private int currentBet;
    private Long currentPlayerTurn;
    private Long dealerPosition;
    private Long smallBlindPosition;
    private Long bigBlindPosition;
    private boolean isGameActive;
    private Long lastActionPlayerId;
    private String lastAction;
}