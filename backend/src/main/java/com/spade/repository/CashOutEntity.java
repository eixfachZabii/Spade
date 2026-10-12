package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The chips a player counted when leaving. The count, not the mirror, is what the ledger keeps (ADR 0006). */
@Entity
@Table(name = "cash_out")
public class CashOutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_id", nullable = false, unique = true)
    private long seatId;

    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private Instant at;

    protected CashOutEntity() {
    }

    public CashOutEntity(long seatId, long points, Instant at) {
        this.seatId = seatId;
        this.points = points;
        this.at = at;
    }
}
