package com.diploma.session_service.dto;

import com.diploma.session_service.entity.SessionEvent;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record SessionEventResponse(
        UUID id,
        UUID sessionId,
        String eventType,
        JsonNode payload,
        UUID userId,
        Instant createdAt
) {
    public static SessionEventResponse from(SessionEvent e) {
        return new SessionEventResponse(
                e.getId(),
                e.getSessionId(),
                e.getEventType(),
                e.getPayload(),
                e.getUserId(),
                e.getCreatedAt()
        );
    }
}
