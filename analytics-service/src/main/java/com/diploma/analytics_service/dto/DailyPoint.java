package com.diploma.analytics_service.dto;

/**
 * Event counts for a single day, split into the dimensions the dashboard charts:
 * sessions created, reviews completed and code executions.
 */
public record DailyPoint(
        String day,
        long sessions,
        long reviews,
        long executions
) {}
