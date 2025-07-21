package com.spadeboot.api.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleChangeDto {
    @NotNull(message = "isAdmin flag cannot be null")
    private Boolean isAdmin;
}