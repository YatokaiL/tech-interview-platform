package com.diploma.review_service.dto;

import com.diploma.review_service.entity.Verdict;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReviewRequest(
        @NotNull UUID sessionId,
        @Min(1) @Max(5) Short scoreCorrectness,
        @Min(1) @Max(5) Short scoreCodeQuality,
        @Min(1) @Max(5) Short scorePerformance,
        @Min(1) @Max(5) Short scoreCommunication,
        Verdict verdict,
        @Size(max = 5000) String summary
) {}
