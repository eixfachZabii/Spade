package com.spadeboot.api.dto.response;

import com.spadeboot.domain.user.PlayerStatus;
import com.spadeboot.domain.user.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerDto {
    private Long id;
    private User user;
    private Integer chips;
    private PlayerStatus status;
    private Long currentTableId;
    private Double winProbability;
    private Integer totalBet;
}
