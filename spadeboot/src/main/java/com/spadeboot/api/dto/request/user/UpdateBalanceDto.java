package com.spadeboot.api.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBalanceDto {
    @NotNull(message = "Amount cannot be null")
    private Integer amount;
}