package com.diploma.subscription_service.dto;

import com.diploma.subscription_service.entity.Subscription;
import com.diploma.subscription_service.entity.SubscriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID companyId,
        UUID tariffId,
        TariffResponse tariff,
        SubscriptionStatus status,
        Integer periodMonths,
        Instant startedAt,
        Instant endsAt,
        Boolean autoRenew,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<PaymentResponse> payments
) {
    public static SubscriptionResponse from(Subscription s, TariffResponse tariff, List<PaymentResponse> payments) {
        return new SubscriptionResponse(
                s.getId(),
                s.getCompanyId(),
                s.getTariffId(),
                tariff,
                s.getStatus(),
                s.getPeriodMonths(),
                s.getStartedAt(),
                s.getEndsAt(),
                s.getAutoRenew(),
                s.getCreatedBy(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                payments
        );
    }
}
