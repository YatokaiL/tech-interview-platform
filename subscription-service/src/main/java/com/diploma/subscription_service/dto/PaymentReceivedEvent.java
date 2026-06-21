package com.diploma.subscription_service.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentReceivedEvent(
        UUID paymentId,
        UUID subscriptionId,
        UUID companyId,
        UUID tariffId,
        UUID createdBy,
        Integer amountCents,
        String currency,
        Instant paidAt
) {}
