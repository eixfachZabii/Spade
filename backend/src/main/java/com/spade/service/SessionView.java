package com.spade.service;

import com.spade.repository.DeviceSessionEntity;
import java.time.Instant;

public record SessionView(long id, String deviceName, Instant createdAt, Instant lastUsedAt) {

    static SessionView of(DeviceSessionEntity session) {
        return new SessionView(session.getId(), session.getDeviceName(), session.getCreatedAt(), session.getLastUsedAt());
    }
}
