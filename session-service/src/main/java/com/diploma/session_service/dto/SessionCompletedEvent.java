package com.diploma.session_service.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionCompletedEvent(
        UUID sessionId,
        UUID companyId,
        UUID interviewerId,
        UUID candidateId,
        Instant completedAt
) {}
