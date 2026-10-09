package com.spadeboot.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private static JwtUtils withSecret(String secret) {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        return utils;
    }

    @Test
    void refusesToStartWithoutASecret() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> withSecret("").validateSecret());
        assertTrue(e.getMessage().contains("SPADE_JWT_SECRET"), e.getMessage());
    }

    @Test
    void refusesASecretShorterThan32Bytes() {
        assertThrows(IllegalStateException.class, () -> withSecret("too-short").validateSecret());
    }

    @Test
    void acceptsA32ByteSecret() {
        assertDoesNotThrow(() -> withSecret("x".repeat(32)).validateSecret());
    }
}
