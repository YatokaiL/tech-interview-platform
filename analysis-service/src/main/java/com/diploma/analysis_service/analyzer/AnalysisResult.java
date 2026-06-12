package com.diploma.analysis_service.analyzer;

import com.diploma.analysis_service.dto.AnalysisIssue;

import java.util.List;

public record AnalysisResult(
        Integer linesOfCode,
        Integer cyclomaticComplexity,
        Integer cognitiveComplexity,
        Integer methodCount,
        String timeComplexity,
        List<AnalysisIssue> issues
) {}
