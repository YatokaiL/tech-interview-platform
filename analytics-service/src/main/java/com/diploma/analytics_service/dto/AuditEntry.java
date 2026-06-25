package com.diploma.analytics_service.dto;

import com.diploma.analytics_service.entity.AnalyticsEvent;

import java.time.Instant;
import java.util.UUID;

/** A row in the audit log view (most recent events first). */
public record AuditEntry(
        UUID id,
        String eventType,
        UUID companyId,
        UUID userId,
        String language,
        String verdict,
        Long valueCents,
        Instant occurredAt
) {
    public static AuditEntry from(AnalyticsEvent e) {
        return new AuditEntry(
                e.getId(),
                e.getEventType(),
                e.getCompanyId(),
                e.getUserId(),
                e.getLanguage(),
                e.getVerdict(),
                e.getValueCents(),
                e.getOccurredAt()
        );
    }
}
