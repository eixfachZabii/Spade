// src/main/java/com/pokerapp/websocket/dto/PlayerActionDto.java
package com.spadeboot.api.dto;

import lombok.Data;

@Data
public class PlayerActionDto {
    public enum ActionType {
        CHECK, CALL, RAISE, FOLD, ALL_IN
    }

    private ActionType action;
    private Integer amount; // For raise or all-in
}