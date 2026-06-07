package com.diploma.review_service.repository;

import com.diploma.review_service.entity.Review;
import com.diploma.review_service.entity.ReviewStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findBySessionId(UUID sessionId);

    List<Review> findByCandidateIdAndStatusOrderByCreatedAtDesc(UUID candidateId, ReviewStatus status, Pageable pageable);

    List<Review> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);
}
