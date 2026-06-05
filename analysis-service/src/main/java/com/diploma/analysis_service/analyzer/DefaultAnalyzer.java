package com.diploma.analysis_service.analyzer;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fallback analyzer for languages without a dedicated implementation.
 * Reports only line count; complexity/issues stay null.
 */
@Component
public class DefaultAnalyzer implements Analyzer {

    @Override
    public String language() {
        return "*";
    }

    @Override
    public AnalysisResult analyze(String code) {
        int loc = countNonBlankLines(code);
        return new AnalysisResult(loc, null, null, null, List.of());
    }

    static int countNonBlankLines(String code) {
        if (code == null || code.isEmpty()) return 0;
        int count = 0;
        for (String line : code.split("\\R", -1)) {
            if (!line.trim().isEmpty()) count++;
        }
        return count;
    }
}
