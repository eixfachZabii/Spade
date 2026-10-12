package com.spade.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterPlayerRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,32}$", message = "use 3 to 32 letters, digits, '.', '_' or '-'")
        String username,
        @NotBlank @Size(max = 40) String displayName,
        @NotNull @Size(min = 8, max = 72, message = "use 8 to 72 characters") String password,
        @NotBlank String inviteCode) {
}
