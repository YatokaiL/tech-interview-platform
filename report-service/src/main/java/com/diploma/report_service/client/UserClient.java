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
public class UserClient {

    private final RestClient http;

    public UserClient(@Value("${app.user-service.uri}") String baseUri) {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public UserInfo fetch(UUID userId, String bearerToken) {
        if (userId == null) return null;
        try {
            return http.get()
                    .uri("/api/users/{id}", userId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(UserInfo.class);
        } catch (HttpStatusCodeException e) {
            log.debug("Failed to fetch user {}: {}", userId, e.getStatusCode());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;

        public String displayName() {
            String fl = "";
            if (firstName != null && !firstName.isBlank()) fl += firstName;
            if (lastName != null && !lastName.isBlank()) fl += (fl.isEmpty() ? "" : " ") + lastName;
            return fl.isBlank() ? (email != null ? email : "?") : fl;
        }
    }
}
