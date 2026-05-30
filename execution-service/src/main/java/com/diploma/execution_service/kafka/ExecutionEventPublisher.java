package com.diploma.execution_service.kafka;

import com.diploma.execution_service.dto.CodeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventPublisher {

    public static final String TOPIC_CODE_EXECUTED = "code-executed";

    private final KafkaTemplate<String, Object> kafka;

    public void publishCodeExecuted(CodeExecutedEvent event) {
        try {
            kafka.send(TOPIC_CODE_EXECUTED, event.executionId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish code-executed event for {}: {}", event.executionId(), e.getMessage());
        }
    }
}
