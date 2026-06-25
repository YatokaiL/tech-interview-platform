package com.diploma.analytics_service.controller;

import com.diploma.analytics_service.dto.AuditEntry;
import com.diploma.analytics_service.dto.BreakdownSlice;
import com.diploma.analytics_service.dto.DailyPoint;
import com.diploma.analytics_service.dto.OverviewResponse;
import com.diploma.analytics_service.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only analytics API. Every endpoint reads from the aggregated event
 * store, so it never touches the operational services directly.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> overview() {
        return ResponseEntity.ok(queryService.overview());
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyPoint>> daily(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(queryService.daily(days));
    }

    @GetMapping("/verdicts")
    public ResponseEntity<List<BreakdownSlice>> verdicts() {
        return ResponseEntity.ok(queryService.verdicts());
    }

    @GetMapping("/languages")
    public ResponseEntity<List<BreakdownSlice>> languages() {
        return ResponseEntity.ok(queryService.languages());
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditEntry>> audit(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(queryService.recent(limit));
    }
}
