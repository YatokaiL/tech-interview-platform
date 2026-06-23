package com.diploma.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side projections of the events published by other services.
 * Only the fields the notification-service cares about are mapped.
 */
public final class Events {

    private Events() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRegistered(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            String role
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionCreated(
            UUID sessionId,
            UUID companyId,
            UUID interviewerId,
            UUID candidateId,
            UUID taskId,
            String language,
            Instant scheduledAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionCompleted(
            UUID sessionId,
            UUID companyId,
            UUID interviewerId,
            UUID candidateId,
            Instant completedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewCompleted(
            UUID reviewId,
            UUID sessionId,
            UUID reviewerId,
            UUID candidateId,
            UUID companyId,
            String verdict,
            Double averageScore,
            Instant submittedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentReceived(
            UUID paymentId,
            UUID subscriptionId,
            UUID companyId,
            UUID tariffId,
            UUID createdBy,
            Integer amountCents,
            String currency,
            Instant paidAt
    ) {}
}
