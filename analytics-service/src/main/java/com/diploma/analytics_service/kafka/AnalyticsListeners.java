package com.diploma.analytics_service.kafka;

import com.diploma.analytics_service.service.EventIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Single fan-in listener: every domain event the platform emits is mirrored into
 * the analytics event store. New topics only need to be added to the list below.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsListeners {

    private final EventIngestService ingestService;

    @KafkaListener(
            topics = {
                    "user-registered",
                    "session-created",
                    "session-completed",
                    "review-completed",
                    "payment-received",
                    "code-executed",
                    "analysis-completed",
                    "notification-requested",
                    "analytics-event"
            },
            groupId = "analytics-service"
    )
    public void onEvent(@Payload(required = false) String payload,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        ingestService.ingest(topic, payload);
    }
}
