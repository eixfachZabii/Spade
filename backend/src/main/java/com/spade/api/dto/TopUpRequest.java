package com.spade.api.dto;

import com.spade.domain.Chips;
import io.swagger.v3.oas.annotations.media.Schema;

/** More chips for the seat you hold, in points. The chip rules are checked by the service, with a clear message. */
public record TopUpRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Points to add: a multiple of 5, the smallest chip.",
                minimum = "" + Chips.SMALLEST, maximum = "" + Chips.MAX, multipleOf = Chips.SMALLEST)
        long points) {
}
