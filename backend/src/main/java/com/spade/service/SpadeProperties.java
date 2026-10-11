package com.spade.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.regex.Pattern;
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
        String inviteCodeSha256,
        @NotNull @Valid Login login) {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    /**
     * The format check lives here, not in a bean-validation {@code @Pattern}: Boot prints a rejected value
     * ("Value: ..." in the startup report, "rejected value [...]" in the exception), and someone who typed
     * the invite code itself where its hash belongs must not see that secret in a log. This message never
     * contains the value. A blank value is left to {@code @NotBlank}, which is safe to print.
     */
    public SpadeProperties {
        if (inviteCodeSha256 != null && !inviteCodeSha256.isBlank() && !SHA_256_HEX.matcher(inviteCodeSha256).matches()) {
            throw new IllegalArgumentException("SPADE_INVITE_CODE_SHA256 (spade.invite-code-sha256) must be the "
                    + "SHA-256 hex of the invite code: 64 lowercase hex digits, not the code itself. "
                    + "Make it with: printf '%s' 'the code' | shasum -a 256");
        }
    }

    /**
     * {@code maxFailures} locks one username; {@code maxFailuresPerAddress} locks one caller address (and one
     * address's wrong invite codes). The address limit is higher: at a poker night every phone reaches the server
     * from the home's one public IP, so one typist must not lock out the table.
     */
    public record Login(@Min(1) int maxFailures, @Min(1) int maxFailuresPerAddress, @NotNull Duration lockout) {
    }
}
