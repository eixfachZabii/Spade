package com.spade.api.dto;

import com.spade.domain.Chips;
import io.swagger.v3.oas.annotations.media.Schema;

public record JoinTableRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Points put on the table: a multiple of 5, the smallest chip.",
                minimum = "" + Chips.SMALLEST, maximum = "" + Chips.MAX, multipleOf = Chips.SMALLEST)
        long buyIn) {
}
