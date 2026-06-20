package com.diploma.subscription_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscriptionRequest(
        @NotNull UUID tariffId,
        @Min(1) @Max(36) Integer periodMonths,
        Boolean autoRenew
) {}
