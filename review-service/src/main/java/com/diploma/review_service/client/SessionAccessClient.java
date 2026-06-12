package com.diploma.review_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class SessionAccessClient {

    private final RestClient http;

    public SessionAccessClient(@Value("${app.session-service.uri}") String baseUri) {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public SessionInfo fetchAccessible(UUID sessionId, String bearerToken) {
        try {
            return http.get()
                    .uri("/api/sessions/{id}", sessionId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(SessionInfo.class);
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            log.debug("session-service rejected access to session {} ({})", sessionId, code);
            if (code == 401 || code == 403 || code == 404) return null;
            throw e;
        }
    }

    public SnapshotInfo fetchLatestSnapshot(UUID sessionId, String bearerToken) {
        try {
            return http.get()
                    .uri("/api/sessions/{id}/snapshots/latest", sessionId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(SnapshotInfo.class);
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            if (code == 204 || code == 404) return null;
            log.debug("Failed to fetch snapshot for {}: {}", sessionId, code);
            return null;
        }
    }

    public TaskInfo fetchTask(UUID taskId, String bearerToken) {
        if (taskId == null) return null;
        try {
            return http.get()
                    .uri("/api/tasks/{id}", taskId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(TaskInfo.class);
        } catch (HttpStatusCodeException e) {
            log.debug("Failed to fetch task {}: {}", taskId, e.getStatusCode());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionInfo {
        private UUID id;
        private UUID companyId;
        private UUID interviewerId;
        private UUID candidateId;
        private UUID taskId;
        private String taskTitle;
        private String language;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnapshotInfo {
        private UUID id;
        private UUID sessionId;
        private String language;
        private String code;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskInfo {
        private UUID id;
        private String title;
        private String language;
        private String expectedOutput;
        private Integer timeLimitMinutes;
    }
}
