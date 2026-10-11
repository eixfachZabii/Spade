package com.spade.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank String username, @NotBlank String password,
                           @NotBlank @Size(max = 60) String deviceName) {
}
