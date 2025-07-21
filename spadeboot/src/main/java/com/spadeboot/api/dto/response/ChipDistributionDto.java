// src/main/java/com/pokerapp/api/dto/ChipDistributionResultDto.java
package com.spadeboot.api.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class ChipDistributionDto {
    private int maxPlayers;
    private int targetValuePerPlayer;
    private Map<String, Integer> distribution;
}