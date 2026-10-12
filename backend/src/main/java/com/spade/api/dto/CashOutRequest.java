package com.spade.api.dto;

import com.spade.domain.Chips;
import io.swagger.v3.oas.annotations.media.Schema;

/** The counted chips when leaving a seat, in points. Nothing (0) is allowed (ADR 0006). */
public record CashOutRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The chips counted at the seat: a multiple of 5, or 0.",
                minimum = "0", maximum = "" + Chips.MAX, multipleOf = Chips.SMALLEST)
        long points) {
}
