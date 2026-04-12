package com.diploma.auth_service.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        List<FieldViolation> violations,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, Instant.now());
    }

    public static ErrorResponse of(String error, String message, List<FieldViolation> violations) {
        return new ErrorResponse(error, message, violations, Instant.now());
    }

    public record FieldViolation(String field, String message) {}
}
