package com.spade.service;

import java.security.SecureRandom;
import java.util.Base64;

/** 256-bit opaque session tokens (ADR 0011). Never log one. */
final class SessionTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionTokens() {
    }

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
