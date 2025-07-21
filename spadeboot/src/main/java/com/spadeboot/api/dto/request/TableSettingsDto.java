package com.spadeboot.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableSettingsDto {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;

    @Size(max = 255, message = "Description can be at most 255 characters")
    private String description;

    @NotNull(message = "Max players must be specified")
    @Min(value = 2, message = "Max players must be at least 2")
    @Max(value = 10, message = "Max players cannot exceed 10")
    private Integer maxPlayers;

    @NotNull(message = "Minimum buy-in must be specified")
    @Min(value = 10, message = "Minimum buy-in must be at least 10")
    private Integer minBuyIn;

    @NotNull(message = "Maximum buy-in must be specified")
    @Min(value = 10, message = "Maximum buy-in must be at least 10")
    private Integer maxBuyIn;

    @NotNull(message = "isPrivate must be specified")
    private Boolean isPrivate;
}