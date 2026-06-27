package com.diploma.analytics_service.repository;

import com.diploma.analytics_service.entity.AnalyticsEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    /** [eventType, count] for every event type — drives the KPI tiles. */
    @Query("SELECT e.eventType, COUNT(e) FROM AnalyticsEvent e GROUP BY e.eventType")
    List<Object[]> countByType();

    /** Total monetary value of confirmed payments, in cents. */
    @Query("SELECT COALESCE(SUM(e.valueCents), 0) FROM AnalyticsEvent e WHERE e.eventType = 'payment-received'")
    long totalRevenueCents();

    /** [verdict, count] over submitted reviews. */
    @Query("SELECT e.verdict, COUNT(e) FROM AnalyticsEvent e " +
           "WHERE e.eventType = 'review-completed' AND e.verdict IS NOT NULL GROUP BY e.verdict")
    List<Object[]> verdictBreakdown();

    /** [language, count] over created sessions. */
    @Query("SELECT e.language, COUNT(e) FROM AnalyticsEvent e " +
           "WHERE e.eventType = 'session-created' AND e.language IS NOT NULL GROUP BY e.language")
    List<Object[]> languageBreakdown();

    /** [yyyy-MM-dd, eventType, count] since a cutoff — the daily time series. */
    @Query(value = """
            SELECT to_char(date_trunc('day', occurred_at), 'YYYY-MM-DD') AS day,
                   event_type,
                   COUNT(*)
            FROM analytics_events
            WHERE occurred_at >= :since
            GROUP BY day, event_type
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> dailyCounts(@Param("since") Instant since);

    List<AnalyticsEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
