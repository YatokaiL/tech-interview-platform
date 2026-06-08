package com.diploma.review_service.dto;

import com.diploma.review_service.entity.Verdict;

import java.time.Instant;
import java.util.UUID;

public record ReviewCompletedEvent(
        UUID reviewId,
        UUID sessionId,
        UUID reviewerId,
        UUID candidateId,
        UUID companyId,
        Verdict verdict,
        Double averageScore,
        Instant submittedAt
) {}
