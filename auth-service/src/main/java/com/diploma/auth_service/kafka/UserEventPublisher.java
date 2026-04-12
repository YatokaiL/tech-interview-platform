package com.diploma.auth_service.kafka;

import com.diploma.auth_service.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    public static final String TOPIC_USER_REGISTERED = "user-registered";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(TOPIC_USER_REGISTERED, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish UserRegistered event for {}: {}",
                                event.userId(), ex.getMessage());
                    } else {
                        log.debug("Published UserRegistered for {}", event.userId());
                    }
                });
    }
}
