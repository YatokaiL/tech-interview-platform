package com.diploma.analysis_service.analyzer;

import com.diploma.analysis_service.dto.AnalysisIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JavaAnalyzer implements Analyzer {

    private final JavaMetricsCalculator metrics;
    private final CheckstyleRunner checkstyle;
    private final PmdRunner pmd;

    @Value("${app.analysis.max-issues:200}")
    private int maxIssues;

    @Override
    public String language() {
        return "java";
    }

    @Override
    public AnalysisResult analyze(String code) {
        int loc = DefaultAnalyzer.countNonBlankLines(code);
        JavaMetricsCalculator.Metrics m = metrics.compute(code);

        List<AnalysisIssue> issues = new ArrayList<>();
        issues.addAll(checkstyle.run(code));
        issues.addAll(pmd.run(code));

        if (issues.size() > maxIssues) {
            issues = new ArrayList<>(issues.subList(0, maxIssues));
        }

        return new AnalysisResult(
                loc,
                m.cyclomatic() > 0 ? m.cyclomatic() : null,
                m.cognitive() > 0 ? m.cognitive() : null,
                m.methodCount() > 0 ? m.methodCount() : null,
                issues
        );
    }
}
