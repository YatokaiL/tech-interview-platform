package com.diploma.report_service.dto;

import com.diploma.report_service.entity.ReportFormat;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportRequest(
        @NotNull UUID reviewId,
        ReportFormat format
) {}
