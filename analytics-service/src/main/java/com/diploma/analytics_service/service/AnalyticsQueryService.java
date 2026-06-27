package com.diploma.analytics_service.service;

import com.diploma.analytics_service.dto.AuditEntry;
import com.diploma.analytics_service.dto.BreakdownSlice;
import com.diploma.analytics_service.dto.DailyPoint;
import com.diploma.analytics_service.dto.OverviewResponse;
import com.diploma.analytics_service.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final AnalyticsEventRepository repository;

    public OverviewResponse overview() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : repository.countByType()) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new OverviewResponse(
                total,
                counts.getOrDefault("user-registered", 0L),
                counts.getOrDefault("session-created", 0L),
                counts.getOrDefault("session-completed", 0L),
                counts.getOrDefault("code-executed", 0L),
                counts.getOrDefault("analysis-completed", 0L),
                counts.getOrDefault("review-completed", 0L),
                counts.getOrDefault("payment-received", 0L),
                repository.totalRevenueCents()
        );
    }

    public List<DailyPoint> daily(int days) {
        int capped = Math.max(1, Math.min(days, 90));
        Instant since = Instant.now().minus(capped, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

        // day -> [sessions, reviews, executions]
        Map<String, long[]> byDay = new TreeMap<>();
        for (Object[] row : repository.dailyCounts(since)) {
            String day = (String) row[0];
            String type = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] bucket = byDay.computeIfAbsent(day, d -> new long[3]);
            switch (type) {
                case "session-created" -> bucket[0] += count;
                case "review-completed" -> bucket[1] += count;
                case "code-executed" -> bucket[2] += count;
                default -> { /* other event types are not charted here */ }
            }
        }
        return byDay.entrySet().stream()
                .map(e -> new DailyPoint(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
    }

    public List<BreakdownSlice> verdicts() {
        return toSlices(repository.verdictBreakdown());
    }

    public List<BreakdownSlice> languages() {
        return toSlices(repository.languageBreakdown());
    }

    public List<AuditEntry> recent(int limit) {
        int capped = Math.max(1, Math.min(limit, 200));
        return repository.findAllByOrderByReceivedAtDesc(PageRequest.of(0, capped)).stream()
                .map(AuditEntry::from)
                .toList();
    }

    private List<BreakdownSlice> toSlices(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new BreakdownSlice((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }
}
