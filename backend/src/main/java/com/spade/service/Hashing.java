package com.spade.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 for secrets that are compared, never stored in clear (invite code, session tokens). */
public final class Hashing {

    private Hashing() {
    }

    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("this JVM has no SHA-256", e);
        }
    }

    /** Constant-time comparison of a value's SHA-256 with a stored hex digest. */
    public static boolean matchesSha256(String value, String expectedHex) {
        return MessageDigest.isEqual(sha256Hex(value).getBytes(StandardCharsets.US_ASCII),
                expectedHex.getBytes(StandardCharsets.US_ASCII));
    }
}
