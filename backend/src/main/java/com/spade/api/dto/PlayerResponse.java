package com.spade.api.dto;

import com.spade.service.PlayerView;

public record PlayerResponse(long id, String username, String displayName) {

    public static PlayerResponse from(PlayerView player) {
        return new PlayerResponse(player.id(), player.username(), player.displayName());
    }
}
