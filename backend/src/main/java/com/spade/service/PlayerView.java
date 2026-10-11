package com.spade.service;

import com.spade.repository.PlayerEntity;

public record PlayerView(long id, String username, String displayName) {

    static PlayerView of(PlayerEntity player) {
        return new PlayerView(player.getId(), player.getUsername(), player.getDisplayName());
    }
}
