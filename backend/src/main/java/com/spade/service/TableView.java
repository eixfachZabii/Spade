package com.spade.service;

import java.util.List;

public record TableView(long id, String name, long hostId, long smallBlind, long bigBlind, int maxSeats,
                        boolean open, List<SeatView> seats) {
}
