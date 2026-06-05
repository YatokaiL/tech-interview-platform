package com.diploma.analysis_service.analyzer;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnalyzerRegistry {

    private final Map<String, Analyzer> byLanguage;
    private final Analyzer fallback;

    public AnalyzerRegistry(List<Analyzer> analyzers, DefaultAnalyzer fallback) {
        this.byLanguage = new HashMap<>();
        for (Analyzer a : analyzers) {
            byLanguage.put(a.language().toLowerCase(), a);
        }
        this.fallback = fallback;
    }

    public Analyzer forLanguage(String language) {
        if (language == null) return fallback;
        Analyzer a = byLanguage.get(language.toLowerCase());
        return a != null ? a : fallback;
    }
}
