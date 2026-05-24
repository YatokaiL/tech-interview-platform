package com.diploma.execution_service.client;

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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionInfo {
        private UUID id;
        private UUID interviewerId;
        private UUID candidateId;
        private String language;
        private String status;
    }
}
