// src/main/java/com/pokerapp/websocket/dto/WinnerDto.java
package com.spadeboot.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class WinnerDto {
    private Long playerId;
    private String username;
    private String handRank;
    private List<String> winningCards;
    private int amountWon;
}