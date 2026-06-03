package com.diploma.analysis_service.analyzer;

import com.diploma.analysis_service.dto.AnalysisIssue;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class CheckstyleRunner {

    @Value("${app.analysis.checkstyle-config}")
    private Resource checkstyleConfig;

    private Configuration configuration;

    @PostConstruct
    public void init() {
        try (InputStream in = checkstyleConfig.getInputStream()) {
            this.configuration = ConfigurationLoader.loadConfiguration(
                    new InputSource(in),
                    new PropertiesExpander(new Properties()),
                    ConfigurationLoader.IgnoredModulesOptions.OMIT
            );
        } catch (Exception e) {
            log.error("Failed to load Checkstyle config", e);
            this.configuration = null;
        }
    }

    public List<AnalysisIssue> run(String code) {
        if (configuration == null) return List.of();
        Path tempDir = null;
        Checker checker = null;
        try {
            tempDir = Files.createTempDirectory("checkstyle-");
            Path file = tempDir.resolve("Main.java");
            Files.writeString(file, code, StandardCharsets.UTF_8);

            checker = new Checker();
            checker.setModuleClassLoader(Checker.class.getClassLoader());
            checker.configure(configuration);

            List<AnalysisIssue> issues = new ArrayList<>();
            checker.addListener(new AuditListener() {
                @Override public void auditStarted(AuditEvent event) {}
                @Override public void auditFinished(AuditEvent event) {}
                @Override public void fileStarted(AuditEvent event) {}
                @Override public void fileFinished(AuditEvent event) {}

                @Override
                public void addError(AuditEvent event) {
                    issues.add(new AnalysisIssue(
                            "checkstyle",
                            event.getSeverityLevel() == null ? "WARNING" : event.getSeverityLevel().getName().toUpperCase(),
                            event.getLine(),
                            event.getSourceName() == null ? null : shortName(event.getSourceName()),
                            event.getMessage()
                    ));
                }

                @Override
                public void addException(AuditEvent event, Throwable throwable) {
                    log.debug("Checkstyle internal exception: {}", throwable.getMessage());
                }
            });

            checker.process(List.of(file.toFile()));
            return issues;
        } catch (Exception e) {
            log.warn("Checkstyle run failed: {}", e.getMessage());
            return List.of();
        } finally {
            if (checker != null) {
                try { checker.destroy(); } catch (Exception ignored) {}
            }
            cleanup(tempDir);
        }
    }

    private static String shortName(String fullyQualified) {
        int dot = fullyQualified.lastIndexOf('.');
        return dot < 0 ? fullyQualified : fullyQualified.substring(dot + 1);
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
