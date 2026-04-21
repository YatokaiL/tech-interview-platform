package com.diploma.user_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-registered", groupId = "user-service")
    public void onUserRegistered(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            log.info("Received UserRegistered: userId={}, email={}, role={}",
                    event.path("userId").asText(),
                    event.path("email").asText(),
                    event.path("role").asText());
            // Placeholder: future audit logging / cache invalidation.
        } catch (Exception ex) {
            log.warn("Failed to parse UserRegistered payload: {}", payload, ex);
        }
    }
}
