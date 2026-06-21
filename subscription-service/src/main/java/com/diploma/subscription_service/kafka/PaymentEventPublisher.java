package com.diploma.subscription_service.kafka;

import com.diploma.subscription_service.dto.PaymentReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    public static final String TOPIC_PAYMENT_RECEIVED = "payment-received";

    private final KafkaTemplate<String, Object> kafka;

    public void publishPaymentReceived(PaymentReceivedEvent event) {
        try {
            kafka.send(TOPIC_PAYMENT_RECEIVED, event.paymentId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish payment-received event for {}: {}", event.paymentId(), e.getMessage());
        }
    }
}
