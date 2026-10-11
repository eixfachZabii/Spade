package com.spade.api.dto;

import com.spade.service.SessionView;
import java.time.Instant;

public record DeviceResponse(long id, String deviceName, Instant createdAt, Instant lastUsedAt, boolean current) {

    public static DeviceResponse from(SessionView session, long currentSessionId) {
        return new DeviceResponse(session.id(), session.deviceName(), session.createdAt(), session.lastUsedAt(),
                session.id() == currentSessionId);
    }
}
