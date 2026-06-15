package com.diploma.report_service.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        List<Violation> violations,
        Instant timestamp
) {
    public record Violation(String field, String message) {}

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, Instant.now());
    }

    public static ErrorResponse of(String error, String message, List<Violation> violations) {
        return new ErrorResponse(error, message, violations, Instant.now());
    }
}
