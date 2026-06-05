package com.diploma.analysis_service.analyzer;

public interface Analyzer {
    String language();

    AnalysisResult analyze(String code);
}
