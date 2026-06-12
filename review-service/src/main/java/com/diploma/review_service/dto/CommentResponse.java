package com.diploma.review_service.dto;

import com.diploma.review_service.entity.ReviewComment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID reviewId,
        UUID authorId,
        Integer lineNumber,
        String text,
        Instant createdAt
) {
    public static CommentResponse from(ReviewComment c) {
        return new CommentResponse(
                c.getId(),
                c.getReviewId(),
                c.getAuthorId(),
                c.getLineNumber(),
                c.getText(),
                c.getCreatedAt()
        );
    }
}
