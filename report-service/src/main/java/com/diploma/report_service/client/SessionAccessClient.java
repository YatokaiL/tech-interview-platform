package com.diploma.report_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
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
            if (code == 401 || code == 403 || code == 404) return null;
            log.warn("session-service error for {}: {}", sessionId, code);
            throw e;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionInfo {
        private UUID id;
        private String title;
        private UUID companyId;
        private UUID interviewerId;
        private UUID candidateId;
        private UUID taskId;
        private String taskTitle;
        private String language;
        private String status;
        private Instant scheduledAt;
        private Instant startedAt;
        private Instant completedAt;
    }
}
