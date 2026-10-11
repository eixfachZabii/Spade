package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One logged-in device. Only the token's SHA-256 is kept (ADR 0011). */
@Entity
@Table(name = "device_session")
public class DeviceSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private long playerId;

    @Column(name = "token_sha256", nullable = false, unique = true, length = 64)
    private String tokenSha256;

    @Column(name = "device_name", nullable = false, length = 60)
    private String deviceName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected DeviceSessionEntity() {
    }

    public DeviceSessionEntity(long playerId, String tokenSha256, String deviceName, Instant now) {
        this.playerId = playerId;
        this.tokenSha256 = tokenSha256;
        this.deviceName = deviceName;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    /** Marks the session used; written at most once a minute to keep requests cheap. */
    public void touch(Instant now) {
        if (lastUsedAt.isBefore(now.minusSeconds(60))) {
            lastUsedAt = now;
        }
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public Long getId() { return id; }
    public long getPlayerId() { return playerId; }
    public String getDeviceName() { return deviceName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
