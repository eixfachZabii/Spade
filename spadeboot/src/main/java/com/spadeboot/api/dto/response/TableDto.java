package com.spadeboot.api.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableDto {
    private Long id;
    private String name;
    private String description;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Integer minBuyIn;
    private Integer maxBuyIn;
    private Boolean isPrivate;
    private Long ownerId;
    private Boolean hasActiveGame;
}
