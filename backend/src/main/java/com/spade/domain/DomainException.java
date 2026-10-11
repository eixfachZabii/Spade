package com.spade.domain;

/**
 * A rule of the game or the app was broken. The message is written for the person at the table and is
 * shown to them as-is; never put internals or secrets in it. {@link Kind} picks the HTTP status in api.
 */
public class DomainException extends RuntimeException {

    public enum Kind { INVALID, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, TOO_MANY_REQUESTS }

    private final Kind kind;

    public DomainException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
