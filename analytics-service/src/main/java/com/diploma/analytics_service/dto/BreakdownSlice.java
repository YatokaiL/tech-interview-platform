package com.diploma.analytics_service.dto;

/** One slice of a categorical breakdown (e.g. a verdict or a language). */
public record BreakdownSlice(
        String label,
        long count
) {}
