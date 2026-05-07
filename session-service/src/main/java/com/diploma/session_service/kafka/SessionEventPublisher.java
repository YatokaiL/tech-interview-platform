package com.diploma.session_service.kafka;

import com.diploma.session_service.dto.SessionCompletedEvent;
import com.diploma.session_service.dto.SessionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventPublisher {

    public static final String TOPIC_SESSION_CREATED   = "session-created";
    public static final String TOPIC_SESSION_COMPLETED = "session-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSessionCreated(SessionCreatedEvent event) {
        send(TOPIC_SESSION_CREATED, event.sessionId().toString(), event);
    }

    public void publishSessionCompleted(SessionCompletedEvent event) {
        send(TOPIC_SESSION_COMPLETED, event.sessionId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((res, ex) -> {
            if (ex != null) {
                log.warn("Failed to publish to {} ({}): {}", topic, key, ex.getMessage());
            } else {
                log.debug("Published to {} key={}", topic, key);
            }
        });
    }
}
