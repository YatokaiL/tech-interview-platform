package com.diploma.analysis_service.kafka;

import com.diploma.analysis_service.dto.AnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisEventPublisher {

    public static final String TOPIC_ANALYSIS_COMPLETED = "analysis-completed";

    private final KafkaTemplate<String, Object> kafka;

    public void publishAnalysisCompleted(AnalysisCompletedEvent event) {
        try {
            kafka.send(TOPIC_ANALYSIS_COMPLETED, event.analysisId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish analysis-completed event for {}: {}", event.analysisId(), e.getMessage());
        }
    }
}
