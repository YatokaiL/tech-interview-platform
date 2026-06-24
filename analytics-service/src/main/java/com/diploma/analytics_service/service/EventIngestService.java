package com.diploma.analytics_service.service;

import com.diploma.analytics_service.entity.AnalyticsEvent;
import com.diploma.analytics_service.repository.AnalyticsEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Turns a raw Kafka JSON payload into a stored {@link AnalyticsEvent}, pulling a
 * few well-known dimensions into typed columns. Event schemas vary across
 * producers, so every field is looked up defensively — a missing field is fine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestService {

    private static final String[] ACTOR_FIELDS =
            {"userId", "candidateId", "createdBy", "reviewerId", "interviewerId"};
    private static final String[] TIME_FIELDS =
            {"finishedAt", "paidAt", "submittedAt", "completedAt", "scheduledAt", "createdAt"};

    private final AnalyticsEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void ingest(String topic, String rawPayload) {
        if (topic == null || topic.isBlank()) return;

        JsonNode node = null;
        if (rawPayload != null && !rawPayload.isBlank()) {
            try {
                node = objectMapper.readTree(rawPayload);
            } catch (Exception e) {
                log.warn("Could not parse payload for topic {}: {}", topic, e.getMessage());
            }
        }

        AnalyticsEvent event = new AnalyticsEvent();
        event.setEventType(topic);
        event.setPayload(node != null ? rawPayload : null);
        event.setCompanyId(uuid(node, "companyId"));
        event.setUserId(firstUuid(node, ACTOR_FIELDS));
        event.setLanguage(text(node, "language"));
        event.setVerdict(text(node, "verdict"));
        event.setValueCents(longValue(node, "amountCents"));
        event.setOccurredAt(firstInstant(node, TIME_FIELDS));

        repository.save(event);
        log.info("Stored analytics event: {} (company={}, user={})",
                topic, event.getCompanyId(), event.getUserId());
    }

    private UUID firstUuid(JsonNode node, String[] fields) {
        for (String f : fields) {
            UUID v = uuid(node, f);
            if (v != null) return v;
        }
        return null;
    }

    private UUID uuid(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private Long longValue(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return v == null || v.isNull() || !v.isNumber() ? null : v.asLong();
    }

    private Instant firstInstant(JsonNode node, String[] fields) {
        for (String f : fields) {
            String raw = text(node, f);
            if (raw == null) continue;
            try {
                return Instant.parse(raw);
            } catch (DateTimeParseException ignored) {
                // not an ISO instant — keep looking
            }
        }
        return Instant.now();
    }
}
