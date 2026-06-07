package com.diploma.review_service.dto;

import com.diploma.review_service.entity.Review;
import com.diploma.review_service.entity.ReviewStatus;
import com.diploma.review_service.entity.Verdict;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID sessionId,
        UUID reviewerId,
        UUID candidateId,
        UUID companyId,
        Short scoreCorrectness,
        Short scoreCodeQuality,
        Short scorePerformance,
        Short scoreCommunication,
        Double scoreAverage,
        Verdict verdict,
        String summary,
        ReviewStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments
) {
    public static ReviewResponse from(Review r, List<CommentResponse> comments) {
        return new ReviewResponse(
                r.getId(),
                r.getSessionId(),
                r.getReviewerId(),
                r.getCandidateId(),
                r.getCompanyId(),
                r.getScoreCorrectness(),
                r.getScoreCodeQuality(),
                r.getScorePerformance(),
                r.getScoreCommunication(),
                averageScore(r),
                r.getVerdict(),
                r.getSummary(),
                r.getStatus(),
                r.getSubmittedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                comments
        );
    }

    private static Double averageScore(Review r) {
        int count = 0;
        int total = 0;
        if (r.getScoreCorrectness() != null)   { total += r.getScoreCorrectness();   count++; }
        if (r.getScoreCodeQuality() != null)   { total += r.getScoreCodeQuality();   count++; }
        if (r.getScorePerformance() != null)   { total += r.getScorePerformance();   count++; }
        if (r.getScoreCommunication() != null) { total += r.getScoreCommunication(); count++; }
        if (count == 0) return null;
        return Math.round((total * 10.0 / count)) / 10.0;
    }
}
