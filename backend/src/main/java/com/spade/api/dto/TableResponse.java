package com.spade.api.dto;

import com.spade.service.TableView;
import java.util.List;

public record TableResponse(long id, String name, long hostId, long smallBlind, long bigBlind, int maxSeats,
                            boolean open, List<SeatResponse> seats) {

    public static TableResponse from(TableView table) {
        return new TableResponse(table.id(), table.name(), table.hostId(), table.smallBlind(), table.bigBlind(),
                table.maxSeats(), table.open(), table.seats().stream().map(SeatResponse::from).toList());
    }
}
