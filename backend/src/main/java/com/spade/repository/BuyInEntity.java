package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Money turned into chips: at sitting down or topping up. Append-only (phase D4). */
@Entity
@Table(name = "buy_in")
public class BuyInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_id", nullable = false)
    private long seatId;

    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private Instant at;

    protected BuyInEntity() {
    }

    public BuyInEntity(long seatId, long points, Instant at) {
        this.seatId = seatId;
        this.points = points;
        this.at = at;
    }
}
