package com.diploma.report_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ReviewClient {

    private final RestClient http;

    public ReviewClient(@Value("${app.review-service.uri}") String baseUri) {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public ReviewInfo fetch(UUID reviewId, String bearerToken) {
        try {
            return http.get()
                    .uri("/api/reviews/{id}", reviewId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(ReviewInfo.class);
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            if (code == 401 || code == 403 || code == 404) return null;
            log.warn("review-service error for {}: {}", reviewId, code);
            throw e;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReviewInfo {
        private UUID id;
        private UUID sessionId;
        private UUID reviewerId;
        private UUID candidateId;
        private UUID companyId;
        private Integer scoreCorrectness;
        private Integer scoreCodeQuality;
        private Integer scorePerformance;
        private Integer scoreCommunication;
        private Double scoreAverage;
        private String verdict;
        private String summary;
        private String status;
        private Instant submittedAt;
        private Instant createdAt;
        private Instant updatedAt;
        private List<Comment> comments;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Comment {
            private UUID id;
            private UUID authorId;
            private Integer lineNumber;
            private String text;
            private Instant createdAt;
        }
    }
}
