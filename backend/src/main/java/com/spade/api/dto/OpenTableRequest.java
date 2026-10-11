package com.spade.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Blinds are in points (CONTEXT.md: Point). Their chip rules are checked by the service, with a clear message. */
public record OpenTableRequest(@NotBlank @Size(max = 40) String name, long smallBlind, long bigBlind,
                               @Min(2) @Max(10) int maxSeats) {
}
