package com.diploma.analysis_service.dto;

import com.diploma.analysis_service.entity.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public record AnalysisCompletedEvent(
        UUID analysisId,
        UUID sessionId,
        UUID userId,
        String language,
        AnalysisStatus status,
        Integer linesOfCode,
        Integer cyclomaticComplexity,
        Integer cognitiveComplexity,
        Integer methodCount,
        Integer issueCount,
        Long durationMs,
        Instant finishedAt
) {}
