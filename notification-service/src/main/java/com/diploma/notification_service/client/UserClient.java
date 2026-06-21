package com.diploma.notification_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    @Value("${app.user-service.uri}")
    private String baseUri;

    private final InternalTokenProvider tokenProvider;
    private RestClient http;

    @PostConstruct
    void init() {
        this.http = RestClient.builder().baseUrl(baseUri).build();
    }

    public UserInfo fetch(UUID userId) {
        if (userId == null) return null;
        try {
            return http.get()
                    .uri("/api/users/{id}/profile", userId)
                    .header("Authorization", "Bearer " + tokenProvider.mint())
                    .retrieve()
                    .body(UserInfo.class);
        } catch (Exception e) {
            log.warn("Failed to fetch user {}: {}", userId, e.getMessage());
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
            return fl.isBlank() ? (email != null ? email : "") : fl;
        }
    }
}
