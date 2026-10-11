package com.spade.service;

/** Who is asking: the player and the device session the request came with. */
public record SpadePrincipal(long playerId, long sessionId) {
}
