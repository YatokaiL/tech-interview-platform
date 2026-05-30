package com.diploma.execution_service.dto;

import com.diploma.execution_service.entity.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record CodeExecutedEvent(
        UUID executionId,
        UUID sessionId,
        UUID userId,
        String language,
        ExecutionStatus status,
        Integer exitCode,
        Long durationMs,
        Instant finishedAt
) {}
