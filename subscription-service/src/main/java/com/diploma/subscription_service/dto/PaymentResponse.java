package com.diploma.subscription_service.dto;

import com.diploma.subscription_service.entity.Payment;
import com.diploma.subscription_service.entity.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID subscriptionId,
        Integer amountCents,
        String currency,
        PaymentStatus status,
        String gatewayRef,
        Instant paidAt,
        Instant createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getSubscriptionId(),
                p.getAmountCents(),
                p.getCurrency(),
                p.getStatus(),
                p.getGatewayRef(),
                p.getPaidAt(),
                p.getCreatedAt()
        );
    }
}
