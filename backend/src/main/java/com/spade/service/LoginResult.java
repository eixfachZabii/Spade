package com.spade.service;

/** The token is returned exactly once, at login; Spade keeps only its hash. */
public record LoginResult(String token, SessionView session) {
}
