package com.diploma.review_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class AnalysisClient {

    private static final ParameterizedTypeReference<List<AnalysisInfo>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public AnalysisClient(@Value("${app.analysis-service.uri}") String baseUri) {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public AnalysisInfo fetchLatest(UUID sessionId, String bearerToken) {
        try {
            List<AnalysisInfo> list = http.get()
                    .uri(uri -> uri.path("/api/analyses").queryParam("sessionId", sessionId).queryParam("limit", 1).build())
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(LIST_TYPE);
            if (list == null || list.isEmpty()) return null;
            return list.get(0);
        } catch (HttpStatusCodeException e) {
            log.debug("Failed to fetch analyses for {}: {}", sessionId, e.getStatusCode());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysisInfo {
        private UUID id;
        private String status;
        private String language;
        private Integer linesOfCode;
        private Integer cyclomaticComplexity;
        private Integer cognitiveComplexity;
        private Integer methodCount;
        private JsonNode issues;
        private String errorMessage;
    }
}
