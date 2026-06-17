package com.diploma.report_service.client;

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
public class SnapshotClient {

    private final RestClient http;

    public SnapshotClient(@Value("${app.session-service.uri}") String baseUri) {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public SnapshotInfo fetchLatest(UUID sessionId, String bearerToken) {
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnapshotInfo {
        private UUID id;
        private UUID sessionId;
        private String language;
        private String code;
    }
}
