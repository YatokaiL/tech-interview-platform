package com.diploma.review_service.kafka;

import com.diploma.review_service.dto.ReviewCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventPublisher {

    public static final String TOPIC_REVIEW_COMPLETED = "review-completed";

    private final KafkaTemplate<String, Object> kafka;

    public void publishReviewCompleted(ReviewCompletedEvent event) {
        try {
            kafka.send(TOPIC_REVIEW_COMPLETED, event.reviewId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish review-completed event for {}: {}", event.reviewId(), e.getMessage());
        }
    }
}
