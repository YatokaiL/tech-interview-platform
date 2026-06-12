package com.diploma.review_service.repository;

import com.diploma.review_service.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, UUID> {

    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(UUID reviewId);

    void deleteByReviewIdAndId(UUID reviewId, UUID id);
}
