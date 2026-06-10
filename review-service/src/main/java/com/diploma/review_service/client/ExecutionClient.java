package com.diploma.review_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ExecutionClient {

    private static final ParameterizedTypeReference<List<ExecutionInfo>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public ExecutionClient(@Value("${app.execution-service.uri}") String baseUri) {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public ExecutionInfo fetchLatest(UUID sessionId, String bearerToken) {
        try {
            List<ExecutionInfo> list = http.get()
                    .uri(uri -> uri.path("/api/executions").queryParam("sessionId", sessionId).queryParam("limit", 1).build())
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(LIST_TYPE);
            if (list == null || list.isEmpty()) return null;
            return list.get(0);
        } catch (HttpStatusCodeException e) {
            log.debug("Failed to fetch executions for {}: {}", sessionId, e.getStatusCode());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExecutionInfo {
        private UUID id;
        private UUID sessionId;
        private String language;
        private String status;
        private String stdout;
        private String stderr;
        private Integer exitCode;
        private Long durationMs;
        private String errorMessage;
    }

    public static List<ExecutionInfo> emptyList() {
        return Collections.emptyList();
    }
}
