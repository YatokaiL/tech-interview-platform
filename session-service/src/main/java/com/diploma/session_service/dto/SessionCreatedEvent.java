package com.diploma.session_service.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionCreatedEvent(
        UUID sessionId,
        UUID companyId,
        UUID interviewerId,
        UUID candidateId,
        UUID taskId,
        String language,
        Instant scheduledAt,
        Instant createdAt
) {}
