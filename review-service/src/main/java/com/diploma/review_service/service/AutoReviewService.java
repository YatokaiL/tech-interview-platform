package com.diploma.review_service.service;

import com.diploma.review_service.client.AnalysisClient;
import com.diploma.review_service.client.ExecutionClient;
import com.diploma.review_service.client.SessionAccessClient;
import com.diploma.review_service.dto.CommentRequest;
import com.diploma.review_service.entity.Verdict;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReviewService {

    private static final int COMMENT_LIMIT = 10;

    public AutoProposal generate(
            SessionAccessClient.SessionInfo session,
            SessionAccessClient.SnapshotInfo snapshot,
            SessionAccessClient.TaskInfo task,
            ExecutionClient.ExecutionInfo execution,
            AnalysisClient.AnalysisInfo analysis
    ) {
        short correctness   = scoreCorrectness(execution, task);
        short codeQuality   = scoreCodeQuality(analysis);
        short performance   = scorePerformance(analysis, execution);
        short communication = scoreCommunication(snapshot);

        Verdict verdict = decideVerdict(correctness, codeQuality, performance, communication);
        List<CommentRequest> comments = issueComments(analysis);
        String summary = buildSummary(session, task, execution, analysis,
                correctness, codeQuality, performance, communication, verdict);

        return new AutoProposal(correctness, codeQuality, performance, communication,
                verdict, summary, comments);
    }

    // ---- per-criterion ------------------------------------------------------

    private short scoreCorrectness(ExecutionClient.ExecutionInfo e, SessionAccessClient.TaskInfo t) {
        if (e == null) return 3;
        String status = e.getStatus();
        if ("TIMEOUT".equalsIgnoreCase(status)) return 1;
        if ("ERROR".equalsIgnoreCase(status) || "FAILURE".equalsIgnoreCase(status)) return 2;
        if (e.getExitCode() != null && e.getExitCode() != 0) return 2;

        String expected = t != null ? trimOrNull(t.getExpectedOutput()) : null;
        if (expected == null) return 4;

        String actual = trimOrNull(e.getStdout());
        if (actual == null) return 3;
        if (actual.equals(expected)) return 5;
        if (actual.contains(expected) || expected.contains(actual)) return 4;
        return 3;
    }

    private short scoreCodeQuality(AnalysisClient.AnalysisInfo a) {
        if (a == null || a.getIssues() == null || !a.getIssues().isArray()) return 3;
        int issues = a.getIssues().size();
        if (issues == 0) return 5;
        if (issues <= 2) return 4;
        if (issues <= 5) return 3;
        if (issues <= 10) return 2;
        return 1;
    }

    private short scorePerformance(AnalysisClient.AnalysisInfo a, ExecutionClient.ExecutionInfo e) {
        short base = 3;
        if (a != null && a.getCyclomaticComplexity() != null) {
            int c = a.getCyclomaticComplexity();
            if (c <= 5) base = 5;
            else if (c <= 10) base = 4;
            else if (c <= 15) base = 3;
            else if (c <= 25) base = 2;
            else base = 1;
        }
        if (e != null && "TIMEOUT".equalsIgnoreCase(e.getStatus())) {
            base = (short) Math.min(base, 2);
        }
        return base;
    }

    private short scoreCommunication(SessionAccessClient.SnapshotInfo snapshot) {
        String code = snapshot == null ? null : snapshot.getCode();
        if (code == null || code.isBlank()) return 3;

        int totalLines = 0;
        int commentLines = 0;
        boolean inBlock = false;
        for (String raw : code.split("\\R", -1)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            totalLines++;
            if (inBlock) {
                commentLines++;
                if (line.contains("*/")) inBlock = false;
                continue;
            }
            if (line.startsWith("//") || line.startsWith("#")) {
                commentLines++;
                continue;
            }
            if (line.startsWith("/*")) {
                commentLines++;
                if (!line.contains("*/")) inBlock = true;
            }
        }
        if (totalLines == 0) return 3;
        double ratio = commentLines / (double) totalLines;
        if (ratio >= 0.10) return 5;
        if (ratio >= 0.05) return 4;
        if (commentLines > 0) return 3;
        return 2;
    }

    private Verdict decideVerdict(short c, short q, short p, short m) {
        double avg = (c + q + p + m) / 4.0;
        if (avg >= 4.0) return Verdict.HIRE;
        if (avg >= 2.75) return Verdict.MAYBE;
        return Verdict.NO_HIRE;
    }

    // ---- comments + summary ------------------------------------------------

    private List<CommentRequest> issueComments(AnalysisClient.AnalysisInfo a) {
        List<CommentRequest> out = new ArrayList<>();
        if (a == null || a.getIssues() == null || !a.getIssues().isArray()) return out;
        for (JsonNode node : a.getIssues()) {
            if (out.size() >= COMMENT_LIMIT) break;
            Integer line = node.hasNonNull("line") ? node.get("line").asInt() : null;
            String tool = node.path("tool").asText("analysis");
            String rule = node.path("rule").asText(null);
            String message = node.path("message").asText("").trim();
            if (message.isBlank()) continue;
            String text = String.format("[%s%s] %s",
                    tool,
                    rule == null || rule.isBlank() ? "" : " · " + rule,
                    message);
            out.add(new CommentRequest(line, text));
        }
        return out;
    }

    private String buildSummary(
            SessionAccessClient.SessionInfo session,
            SessionAccessClient.TaskInfo task,
            ExecutionClient.ExecutionInfo execution,
            AnalysisClient.AnalysisInfo analysis,
            short correctness, short codeQuality, short performance, short communication,
            Verdict verdict
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Авто-оценка по данным запуска кода и статического анализа.\n\n");

        sb.append("Корректность (").append(correctness).append("/5): ");
        if (execution == null) {
            sb.append("код ни разу не запускали — оценка нейтральная.");
        } else {
            sb.append("последний запуск — ").append(execution.getStatus());
            if (execution.getExitCode() != null) sb.append(", exit=").append(execution.getExitCode());
            if (execution.getDurationMs() != null) sb.append(", ").append(execution.getDurationMs()).append(" мс");
            sb.append(".");
            if (task != null && task.getExpectedOutput() != null && !task.getExpectedOutput().isBlank()) {
                sb.append(" Ожидаемый вывод задачи ");
                String actual = trimOrNull(execution.getStdout());
                String expected = trimOrNull(task.getExpectedOutput());
                if (expected != null && expected.equals(actual)) sb.append("совпал");
                else if (actual != null && expected != null && (actual.contains(expected) || expected.contains(actual))) sb.append("частично совпал");
                else sb.append("не совпал");
                sb.append(".");
            }
        }
        sb.append("\n");

        sb.append("Чистота кода (").append(codeQuality).append("/5): ");
        if (analysis == null) {
            sb.append("анализ не запускали.");
        } else {
            int issueCount = analysis.getIssues() != null && analysis.getIssues().isArray() ? analysis.getIssues().size() : 0;
            sb.append("Checkstyle/PMD нашли ").append(issueCount).append(" замечани")
                    .append(russianPlural(issueCount, "е", "я", "й")).append(".");
        }
        sb.append("\n");

        sb.append("Производительность (").append(performance).append("/5): ");
        if (analysis != null && analysis.getCyclomaticComplexity() != null) {
            sb.append("цикломатическая сложность ").append(analysis.getCyclomaticComplexity());
            if (analysis.getCognitiveComplexity() != null) sb.append(", когнитивная ").append(analysis.getCognitiveComplexity());
            if (analysis.getTimeComplexity() != null && !analysis.getTimeComplexity().isBlank()) {
                sb.append(", оценка алгоритмической сложности — ").append(analysis.getTimeComplexity());
            }
            sb.append(".");
        } else {
            sb.append("нет данных по сложности.");
        }
        if (execution != null && "TIMEOUT".equalsIgnoreCase(execution.getStatus())) {
            sb.append(" Запуск упёрся в таймаут — оценка снижена.");
        }
        sb.append("\n");

        sb.append("Коммуникация (").append(communication).append("/5): ");
        sb.append("оценено по плотности комментариев в исходнике.\n\n");

        sb.append("Рекомендуемый вердикт: ").append(verdict.name()).append(".\n");
        sb.append("Это автоматическая оценка — отредактируйте её перед отправкой при необходимости.");
        return sb.toString();
    }

    // ---- helpers ------------------------------------------------------------

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String russianPlural(int n, String one, String few, String many) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 14) return many;
        if (mod10 == 1) return one;
        if (mod10 >= 2 && mod10 <= 4) return few;
        return many;
    }

    public record AutoProposal(
            short scoreCorrectness,
            short scoreCodeQuality,
            short scorePerformance,
            short scoreCommunication,
            Verdict verdict,
            String summary,
            List<CommentRequest> comments
    ) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "AutoProposal{c=%d q=%d p=%d m=%d verdict=%s comments=%d}",
                    scoreCorrectness, scoreCodeQuality, scorePerformance, scoreCommunication,
                    verdict, comments.size());
        }
    }
}
