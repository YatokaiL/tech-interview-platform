package com.diploma.execution_service.runner;

import com.diploma.execution_service.entity.ExecutionStatus;

public record ExecutionResult(
        ExecutionStatus status,
        Integer exitCode,
        String stdout,
        String stderr,
        long durationMs,
        String errorMessage
) {}
