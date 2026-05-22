package com.diploma.collaboration_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class SessionClient {

    private final RestClient http;

    public SessionClient(@Value("${app.session-service.uri}") String baseUri) {
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
            HttpStatusCode code = e.getStatusCode();
            log.warn("session-service rejected access to session {} ({})", sessionId, code.value());
            if (code.value() == 401 || code.value() == 403 || code.value() == 404) {
                return null;
            }
            throw e;
        }
    }

    public LatestSnapshot fetchLatestSnapshot(UUID sessionId, String bearerToken) {
        try {
            return http.get()
                    .uri("/api/sessions/{id}/snapshots/latest", sessionId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(LatestSnapshot.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 204 || e.getStatusCode().value() == 404) {
                return null;
            }
            log.warn("Failed to load latest snapshot for {}: {}", sessionId, e.getStatusCode());
            return null;
        }
    }

    public void appendEvent(UUID sessionId, String bearerToken, String eventType, Object payload) {
        try {
            http.post()
                    .uri("/api/sessions/{id}/events", sessionId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Content-Type", "application/json")
                    .body(new EventBody(eventType, payload))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to append session event ({}, {}): {}", sessionId, eventType, e.getStatusCode());
        }
    }

    private record EventBody(String eventType, Object payload) {}

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionInfo {
        private UUID id;
        private String title;
        private UUID interviewerId;
        private UUID candidateId;
        private String language;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatestSnapshot {
        private UUID id;
        private UUID sessionId;
        private String language;
        private String code;
        private Instant createdAt;
    }
}
