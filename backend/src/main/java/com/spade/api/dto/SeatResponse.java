package com.spade.api.dto;

import com.spade.service.SeatView;

public record SeatResponse(int seatNumber, long playerId, String displayName, long stack) {

    public static SeatResponse from(SeatView seat) {
        return new SeatResponse(seat.seatNumber(), seat.playerId(), seat.displayName(), seat.stack());
    }
}
