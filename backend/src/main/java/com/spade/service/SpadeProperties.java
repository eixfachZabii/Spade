package com.spade.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Spade's own settings, checked at startup. The invite code itself is never configured, only its SHA-256
 * (CLAUDE.md: compare secrets by hash). Make it with: printf '%s' 'the code' | shasum -a 256
 */
@Validated
@ConfigurationProperties("spade")
public record SpadeProperties(
        @NotBlank(message = "set SPADE_INVITE_CODE_SHA256 (the SHA-256 hex of the group's invite code)")
        @Pattern(regexp = "[0-9a-f]{64}", message = "must be the SHA-256 hex of the invite code (64 lowercase hex digits)")
        String inviteCodeSha256,
        @NotNull @Valid Login login) {

    public record Login(@Min(1) int maxFailures, @NotNull Duration lockout) {
    }
}
