package com.diploma.analysis_service.analyzer;

import com.diploma.analysis_service.dto.AnalysisIssue;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PmdRunner {

    @Value("${app.analysis.pmd-ruleset}")
    private Resource pmdRuleset;

    private Path rulesetFile;

    @PostConstruct
    public void init() throws Exception {
        Path tmp = Files.createTempFile("pmd-ruleset-", ".xml");
        try (InputStream in = pmdRuleset.getInputStream()) {
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        tmp.toFile().deleteOnExit();
        this.rulesetFile = tmp;
    }

    public List<AnalysisIssue> run(String code) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("pmd-");
            Path file = tempDir.resolve("Main.java");
            Files.writeString(file, code, StandardCharsets.UTF_8);

            PMDConfiguration config = new PMDConfiguration();
            config.addRuleSet(rulesetFile.toString());
            config.setMinimumPriority(RulePriority.MEDIUM);
            config.addInputPath(file);

            List<AnalysisIssue> issues = new ArrayList<>();
            try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
                Report report = pmd.performAnalysisAndCollectReport();
                for (RuleViolation v : report.getViolations()) {
                    issues.add(new AnalysisIssue(
                            "pmd",
                            v.getRule().getPriority().getName().toUpperCase(),
                            v.getBeginLine(),
                            v.getRule().getName(),
                            v.getDescription()
                    ));
                }
            }
            return issues;
        } catch (Exception e) {
            log.warn("PMD run failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanup(tempDir);
        }
    }

    private static void cleanup(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
    }
}
