package com.spade.api.dto;

import com.spade.domain.Chips;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Blinds are in points (CONTEXT.md: Point). Their chip rules are checked by the service, with a clear message. */
public record OpenTableRequest(
        @NotBlank @Size(max = 40) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "" + Chips.SMALLEST, maximum = "" + Chips.MAX,
                multipleOf = Chips.SMALLEST, description = "Points: a multiple of 5.")
        long smallBlind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "" + Chips.SMALLEST, maximum = "" + Chips.MAX,
                multipleOf = Chips.SMALLEST, description = "Points: a multiple of 5, at least the small blind.")
        long bigBlind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Min(2) @Max(10) int maxSeats) {
}
