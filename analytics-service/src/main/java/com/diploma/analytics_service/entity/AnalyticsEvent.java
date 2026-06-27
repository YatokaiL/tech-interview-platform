package com.diploma.analytics_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per domain event consumed from Kafka — the platform's audit log and
 * the source for all aggregated metrics. A handful of common dimensions
 * (company, actor, language, verdict, monetary value) are extracted into typed
 * columns so the dashboard queries stay simple; the full event is kept as JSONB.
 */
@Entity
@Table(name = "analytics_events")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 40)
    private String language;

    @Column(length = 40)
    private String verdict;

    @Column(name = "value_cents")
    private Long valueCents;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (receivedAt == null) receivedAt = Instant.now();
        if (occurredAt == null) occurredAt = receivedAt;
    }
}
