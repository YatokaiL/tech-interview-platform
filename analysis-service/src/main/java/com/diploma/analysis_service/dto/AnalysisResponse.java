package com.diploma.analysis_service.dto;

import com.diploma.analysis_service.entity.AnalysisStatus;
import com.diploma.analysis_service.entity.CodeAnalysis;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AnalysisResponse(
        UUID id,
        UUID sessionId,
        UUID userId,
        String language,
        AnalysisStatus status,
        Integer linesOfCode,
        Integer cyclomaticComplexity,
        Integer cognitiveComplexity,
        Integer methodCount,
        String timeComplexity,
        JsonNode issues,
        Long durationMs,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static AnalysisResponse from(CodeAnalysis a) {
        return new AnalysisResponse(
                a.getId(),
                a.getSessionId(),
                a.getUserId(),
                a.getLanguage(),
                a.getStatus(),
                a.getLinesOfCode(),
                a.getCyclomaticComplexity(),
                a.getCognitiveComplexity(),
                a.getMethodCount(),
                a.getTimeComplexity(),
                a.getIssues(),
                a.getDurationMs(),
                a.getErrorMessage(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
