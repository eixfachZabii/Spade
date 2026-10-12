package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "poker_table")
public class TableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "host_id", nullable = false)
    private long hostId;

    @Column(name = "small_blind", nullable = false)
    private long smallBlind;

    @Column(name = "big_blind", nullable = false)
    private long bigBlind;

    @Column(name = "max_seats", nullable = false)
    private int maxSeats;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected TableEntity() {
    }

    public TableEntity(String name, long hostId, long smallBlind, long bigBlind, int maxSeats, Instant openedAt) {
        this.name = name;
        this.hostId = hostId;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.maxSeats = maxSeats;
        this.openedAt = openedAt;
    }

    public void close(Instant now) {
        this.closedAt = now;
    }

    public boolean isOpen() { return closedAt == null; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public long getHostId() { return hostId; }
    public long getSmallBlind() { return smallBlind; }
    public long getBigBlind() { return bigBlind; }
    public int getMaxSeats() { return maxSeats; }
}
