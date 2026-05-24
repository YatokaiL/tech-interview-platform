package com.diploma.execution_service.dto;

import com.diploma.execution_service.entity.CodeExecution;
import com.diploma.execution_service.entity.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        UUID sessionId,
        UUID userId,
        String language,
        ExecutionStatus status,
        String stdout,
        String stderr,
        Integer exitCode,
        Long durationMs,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExecutionResponse from(CodeExecution e) {
        return new ExecutionResponse(
                e.getId(),
                e.getSessionId(),
                e.getUserId(),
                e.getLanguage(),
                e.getStatus(),
                e.getStdout(),
                e.getStderr(),
                e.getExitCode(),
                e.getDurationMs(),
                e.getErrorMessage(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
