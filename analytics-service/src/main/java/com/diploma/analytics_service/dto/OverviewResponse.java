package com.diploma.analytics_service.dto;

/**
 * Headline KPIs for the admin dashboard. Counts are derived from the stored
 * event stream, so they reflect everything that has happened on the platform.
 */
public record OverviewResponse(
        long totalEvents,
        long usersRegistered,
        long sessionsCreated,
        long sessionsCompleted,
        long codeExecutions,
        long analysesCompleted,
        long reviewsCompleted,
        long paymentsReceived,
        long revenueCents
) {}
