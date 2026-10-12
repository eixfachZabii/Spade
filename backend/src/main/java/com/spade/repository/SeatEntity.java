package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A player's place at a table. The stack mirrors the chips in front of them (ADR 0006). */
@Entity
@Table(name = "seat")
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private long tableId;

    @Column(name = "player_id", nullable = false)
    private long playerId;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(nullable = false)
    private long stack;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected SeatEntity() {
    }

    public SeatEntity(long tableId, long playerId, int seatNumber, long stack, Instant joinedAt) {
        this.tableId = tableId;
        this.playerId = playerId;
        this.seatNumber = seatNumber;
        this.stack = stack;
        this.joinedAt = joinedAt;
    }

    public void addToStack(long points) {
        stack += points;
    }

    public void leave(Instant now) {
        leftAt = now;
        stack = 0;
    }

    public Long getId() { return id; }
    public long getTableId() { return tableId; }
    public long getPlayerId() { return playerId; }
    public int getSeatNumber() { return seatNumber; }
    public long getStack() { return stack; }
}
