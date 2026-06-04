package com.diploma.analysis_service.dto;

public record AnalysisIssue(
        String tool,
        String severity,
        Integer line,
        String rule,
        String message
) {}
