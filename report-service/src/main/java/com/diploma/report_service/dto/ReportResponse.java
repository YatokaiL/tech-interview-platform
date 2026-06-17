package com.diploma.report_service.dto;

import com.diploma.report_service.entity.Report;
import com.diploma.report_service.entity.ReportFormat;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID sessionId,
        UUID reviewId,
        ReportFormat format,
        Long sizeBytes,
        UUID generatedBy,
        Instant createdAt
) {
    public static ReportResponse from(Report r) {
        return new ReportResponse(
                r.getId(),
                r.getSessionId(),
                r.getReviewId(),
                r.getFormat(),
                r.getSizeBytes(),
                r.getGeneratedBy(),
                r.getCreatedAt()
        );
    }
}
