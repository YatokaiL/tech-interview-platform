package com.diploma.review_service.service;

import com.diploma.review_service.client.AnalysisClient;
import com.diploma.review_service.client.ExecutionClient;
import com.diploma.review_service.client.SessionAccessClient;
import com.diploma.review_service.dto.CommentRequest;
import com.diploma.review_service.entity.Verdict;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class LlmReviewProvider {

    private final ObjectMapper mapper;

    @Value("${app.llm.enabled:false}")
    private boolean enabled;

    @Value("${app.llm.base-url:}")
    private String baseUrl;

    @Value("${app.llm.model:qwen2.5-coder:7b}")
    private String model;

    @Value("${app.llm.timeout-seconds:180}")
    private int timeoutSeconds;

    @Value("${app.llm.temperature:0.2}")
    private double temperature;

    @Value("${app.llm.max-code-chars:8000}")
    private int maxCodeChars;

    private RestClient http;

    public LlmReviewProvider(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    void init() {
        if (!enabled || baseUrl == null || baseUrl.isBlank()) {
            log.info("LLM review provider disabled (enabled={}, baseUrl='{}')", enabled, baseUrl);
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        log.info("LLM review provider enabled: url={}, model={}, timeout={}s", baseUrl, model, timeoutSeconds);
    }

    public boolean isEnabled() {
        return enabled && http != null;
    }

    public AutoReviewService.AutoProposal tryGenerate(
            SessionAccessClient.SessionInfo session,
            SessionAccessClient.SnapshotInfo snapshot,
            SessionAccessClient.TaskInfo task,
            ExecutionClient.ExecutionInfo execution,
            AnalysisClient.AnalysisInfo analysis
    ) {
        if (!isEnabled()) return null;
        long start = System.currentTimeMillis();
        try {
            String prompt = buildPrompt(session, snapshot, task, execution, analysis);

            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);
            body.put("format", "json");
            ObjectNode options = body.putObject("options");
            options.put("temperature", temperature);
            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            JsonNode response = http.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) return null;

            String content = response.path("message").path("content").asText("");
            if (content.isBlank()) {
                log.warn("LLM returned empty content");
                return null;
            }
            JsonNode parsed = mapper.readTree(content);
            AutoReviewService.AutoProposal proposal = mapProposal(parsed);
            log.info("LLM review generated in {} ms ({})", System.currentTimeMillis() - start, proposal);
            return proposal;
        } catch (Exception e) {
            log.warn("LLM call failed ({}), falling back to heuristic", e.toString());
            return null;
        }
    }

    private AutoReviewService.AutoProposal mapProposal(JsonNode root) {
        short c = clampScore(root.path("scoreCorrectness").asInt(3));
        short q = clampScore(root.path("scoreCodeQuality").asInt(3));
        short p = clampScore(root.path("scorePerformance").asInt(3));
        short m = clampScore(root.path("scoreCommunication").asInt(3));
        Verdict verdict = parseVerdict(root.path("verdict").asText("MAYBE"));
        String timeComplexity = trim(root.path("timeComplexity").asText(null));
        String summary = trim(root.path("summary").asText(""));
        if (summary == null) summary = "";
        if (timeComplexity != null && !timeComplexity.isBlank()) {
            summary = "Сгенерировано LLM. Оценка алгоритмической сложности: " + timeComplexity + ".\n\n" + summary;
        } else {
            summary = "Сгенерировано LLM.\n\n" + summary;
        }
        summary += "\n\nЭто автоматическая оценка — отредактируйте её перед отправкой при необходимости.";

        List<CommentRequest> comments = new ArrayList<>();
        JsonNode commentsNode = root.path("comments");
        if (commentsNode.isArray()) {
            for (JsonNode node : commentsNode) {
                if (comments.size() >= 10) break;
                String text = trim(node.path("text").asText(""));
                if (text == null || text.isBlank()) continue;
                Integer line = node.hasNonNull("line") ? node.get("line").asInt() : null;
                if (line != null && line <= 0) line = null;
                comments.add(new CommentRequest(line, text));
            }
        }
        return new AutoReviewService.AutoProposal(c, q, p, m, verdict, summary, comments);
    }

    private short clampScore(int n) {
        if (n < 1) return 1;
        if (n > 5) return 5;
        return (short) n;
    }

    private Verdict parseVerdict(String value) {
        if (value == null) return Verdict.MAYBE;
        try {
            return Verdict.valueOf(value.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return Verdict.MAYBE;
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String buildPrompt(
            SessionAccessClient.SessionInfo session,
            SessionAccessClient.SnapshotInfo snapshot,
            SessionAccessClient.TaskInfo task,
            ExecutionClient.ExecutionInfo execution,
            AnalysisClient.AnalysisInfo analysis
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты опытный технический интервьюер. Оцени работу кандидата на собеседовании ");
        sb.append("по сигналам с платформы и выдай рекомендацию.\n\n");

        sb.append("ЯЗЫК: ").append(session.getLanguage() != null ? session.getLanguage() : "(не указан)").append("\n");
        sb.append("КОД КАНДИДАТА:\n```\n");
        String code = snapshot != null ? snapshot.getCode() : "";
        if (code == null) code = "";
        if (code.length() > maxCodeChars) {
            code = code.substring(0, maxCodeChars) + "\n/* ...truncated... */";
        }
        sb.append(code).append("\n```\n\n");

        if (task != null && task.getExpectedOutput() != null && !task.getExpectedOutput().isBlank()) {
            sb.append("ОЖИДАЕМЫЙ ВЫВОД ЗАДАЧИ:\n").append(task.getExpectedOutput()).append("\n\n");
        }

        if (execution != null) {
            sb.append("ПОСЛЕДНИЙ ЗАПУСК КОДА:\n");
            sb.append("status=").append(execution.getStatus());
            if (execution.getExitCode() != null) sb.append(", exitCode=").append(execution.getExitCode());
            if (execution.getDurationMs() != null) sb.append(", duration=").append(execution.getDurationMs()).append("ms");
            sb.append("\n");
            String stdout = execution.getStdout() == null ? "" : execution.getStdout();
            String stderr = execution.getStderr() == null ? "" : execution.getStderr();
            if (stdout.length() > 1000) stdout = stdout.substring(0, 1000) + "...";
            if (stderr.length() > 1000) stderr = stderr.substring(0, 1000) + "...";
            sb.append("stdout: ").append(stdout).append("\n");
            sb.append("stderr: ").append(stderr).append("\n\n");
        }

        if (analysis != null) {
            sb.append("СТАТИЧЕСКИЙ АНАЛИЗ:\n");
            if (analysis.getLinesOfCode() != null) sb.append("- lines of code: ").append(analysis.getLinesOfCode()).append("\n");
            if (analysis.getMethodCount() != null) sb.append("- methods: ").append(analysis.getMethodCount()).append("\n");
            if (analysis.getCyclomaticComplexity() != null) sb.append("- cyclomatic complexity: ").append(analysis.getCyclomaticComplexity()).append("\n");
            if (analysis.getCognitiveComplexity() != null) sb.append("- cognitive complexity: ").append(analysis.getCognitiveComplexity()).append("\n");
            if (analysis.getTimeComplexity() != null) sb.append("- estimated Big-O: ").append(analysis.getTimeComplexity()).append("\n");
            JsonNode issues = analysis.getIssues();
            if (issues != null && issues.isArray() && issues.size() > 0) {
                sb.append("- найденные замечания (").append(issues.size()).append(") :\n");
                int shown = 0;
                for (JsonNode issue : issues) {
                    if (shown >= 30) break;
                    sb.append("  - [").append(issue.path("tool").asText("?")).append("] ");
                    if (issue.hasNonNull("line")) sb.append("line ").append(issue.get("line").asInt()).append(": ");
                    sb.append(issue.path("message").asText("")).append("\n");
                    shown++;
                }
            }
            sb.append("\n");
        }

        sb.append("ЗАДАЧА:\n");
        sb.append("Поставь баллы 1..5 по 4 критериям: scoreCorrectness, scoreCodeQuality, scorePerformance, scoreCommunication.\n");
        sb.append("Выбери вердикт: HIRE / MAYBE / NO_HIRE.\n");
        sb.append("Оцени алгоритмическую сложность (timeComplexity) в нотации Big-O, например \"O(n)\" или \"O(n^2)\".\n");
        sb.append("Сформируй до 10 комментариев к КОНКРЕТНЫМ строкам кода (поле line — номер строки начиная с 1).\n");
        sb.append("Напиши развёрнутый summary НА РУССКОМ языке: 1-2 абзаца с обоснованием каждого балла и итогом.\n\n");

        sb.append("ОТВЕТ СТРОГО JSON, без markdown, без преамбулы:\n");
        sb.append("{\n");
        sb.append("  \"scoreCorrectness\": 1..5,\n");
        sb.append("  \"scoreCodeQuality\": 1..5,\n");
        sb.append("  \"scorePerformance\": 1..5,\n");
        sb.append("  \"scoreCommunication\": 1..5,\n");
        sb.append("  \"verdict\": \"HIRE\" | \"MAYBE\" | \"NO_HIRE\",\n");
        sb.append("  \"timeComplexity\": \"O(...)\",\n");
        sb.append("  \"summary\": \"строка на русском\",\n");
        sb.append("  \"comments\": [ { \"line\": int, \"text\": \"русский комментарий\" } ]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
