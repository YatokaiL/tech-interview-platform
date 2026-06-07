package com.diploma.review_service.service;

import com.diploma.review_service.client.SessionAccessClient;
import com.diploma.review_service.dto.CommentRequest;
import com.diploma.review_service.dto.CommentResponse;
import com.diploma.review_service.dto.ReviewCompletedEvent;
import com.diploma.review_service.dto.ReviewRequest;
import com.diploma.review_service.dto.ReviewResponse;
import com.diploma.review_service.dto.ReviewUpdateRequest;
import com.diploma.review_service.entity.Review;
import com.diploma.review_service.entity.ReviewComment;
import com.diploma.review_service.entity.ReviewStatus;
import com.diploma.review_service.exception.ForbiddenException;
import com.diploma.review_service.exception.NotFoundException;
import com.diploma.review_service.exception.ValidationException;
import com.diploma.review_service.kafka.ReviewEventPublisher;
import com.diploma.review_service.repository.ReviewCommentRepository;
import com.diploma.review_service.repository.ReviewRepository;
import com.diploma.review_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final SessionAccessClient sessionAccessClient;
    private final ReviewEventPublisher eventPublisher;

    // ---------- READ ----------

    @Transactional(readOnly = true)
    public ReviewResponse getById(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review", id));
        ensureCanView(review);
        return ReviewResponse.from(review, loadCommentResponses(review.getId()));
    }

    @Transactional(readOnly = true)
    public Optional<ReviewResponse> findBySessionId(UUID sessionId) {
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(sessionId, CurrentUser.rawToken());
        if (info == null) throw new ForbiddenException("Session is not accessible to current user");
        return reviewRepository.findBySessionId(sessionId)
                .map(r -> {
                    ensureCanView(r);
                    return ReviewResponse.from(r, loadCommentResponses(r.getId()));
                });
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForCurrentUser(int limit) {
        int capped = Math.max(1, Math.min(limit, 100));
        List<Review> rows;
        if (CurrentUser.isCandidate()) {
            rows = reviewRepository.findByCandidateIdAndStatusOrderByCreatedAtDesc(
                    CurrentUser.id(), ReviewStatus.SUBMITTED, PageRequest.of(0, capped));
        } else if (CurrentUser.isHrManager() || CurrentUser.isAdmin()) {
            UUID companyId = CurrentUser.companyId();
            if (CurrentUser.isAdmin() && companyId == null) {
                rows = reviewRepository.findAll(PageRequest.of(0, capped)).getContent();
            } else if (companyId != null) {
                rows = reviewRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, PageRequest.of(0, capped));
            } else {
                rows = List.of();
            }
        } else if (CurrentUser.isInterviewer()) {
            rows = reviewRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUser.companyId(), PageRequest.of(0, capped));
        } else {
            rows = List.of();
        }
        return rows.stream()
                .map(r -> ReviewResponse.from(r, List.of()))
                .toList();
    }

    // ---------- WRITE ----------

    @Transactional
    public ReviewResponse create(ReviewRequest request) {
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(request.sessionId(), CurrentUser.rawToken());
        if (info == null) throw new ForbiddenException("Session is not accessible to current user");
        if (!"COMPLETED".equals(info.getStatus())) {
            throw new ValidationException("Review can be created only for a COMPLETED session");
        }
        ensureIsInterviewerOf(info);

        if (reviewRepository.findBySessionId(request.sessionId()).isPresent()) {
            throw new ValidationException("Review for this session already exists");
        }

        Review review = new Review();
        review.setSessionId(request.sessionId());
        review.setReviewerId(CurrentUser.id());
        review.setCandidateId(info.getCandidateId());
        review.setCompanyId(info.getCompanyId());
        review.setScoreCorrectness(request.scoreCorrectness());
        review.setScoreCodeQuality(request.scoreCodeQuality());
        review.setScorePerformance(request.scorePerformance());
        review.setScoreCommunication(request.scoreCommunication());
        review.setVerdict(request.verdict());
        review.setSummary(request.summary());
        review.setStatus(ReviewStatus.DRAFT);

        review = reviewRepository.save(review);
        return ReviewResponse.from(review, List.of());
    }

    @Transactional
    public ReviewResponse update(UUID id, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review", id));
        ensureCanEdit(review);
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            throw new ValidationException("Submitted reviews cannot be edited");
        }

        if (request.scoreCorrectness() != null)   review.setScoreCorrectness(request.scoreCorrectness());
        if (request.scoreCodeQuality() != null)   review.setScoreCodeQuality(request.scoreCodeQuality());
        if (request.scorePerformance() != null)   review.setScorePerformance(request.scorePerformance());
        if (request.scoreCommunication() != null) review.setScoreCommunication(request.scoreCommunication());
        if (request.verdict() != null)            review.setVerdict(request.verdict());
        if (request.summary() != null)            review.setSummary(request.summary());

        return ReviewResponse.from(review, loadCommentResponses(review.getId()));
    }

    @Transactional
    public ReviewResponse submit(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review", id));
        ensureCanEdit(review);
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            throw new ValidationException("Review is already submitted");
        }
        if (review.getScoreCorrectness() == null
                || review.getScoreCodeQuality() == null
                || review.getScorePerformance() == null
                || review.getScoreCommunication() == null) {
            throw new ValidationException("All four scores must be set before submitting");
        }
        if (review.getVerdict() == null) {
            throw new ValidationException("Verdict must be set before submitting");
        }

        Instant now = Instant.now();
        review.setStatus(ReviewStatus.SUBMITTED);
        review.setSubmittedAt(now);

        ReviewResponse response = ReviewResponse.from(review, loadCommentResponses(review.getId()));

        eventPublisher.publishReviewCompleted(new ReviewCompletedEvent(
                review.getId(),
                review.getSessionId(),
                review.getReviewerId(),
                review.getCandidateId(),
                review.getCompanyId(),
                review.getVerdict(),
                response.scoreAverage(),
                now
        ));
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review", id));
        ensureCanEdit(review);
        if (review.getStatus() == ReviewStatus.SUBMITTED && !CurrentUser.isAdmin()) {
            throw new ValidationException("Submitted reviews can be removed only by an admin");
        }
        reviewRepository.delete(review);
    }

    // ---------- COMMENTS ----------

    @Transactional
    public CommentResponse addComment(UUID reviewId, CommentRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));
        ensureCanEdit(review);
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            throw new ValidationException("Cannot add comments to a submitted review");
        }

        ReviewComment c = new ReviewComment();
        c.setReviewId(review.getId());
        c.setLineNumber(request.lineNumber());
        c.setText(request.text());
        c.setAuthorId(CurrentUser.id());
        c = commentRepository.save(c);
        return CommentResponse.from(c);
    }

    @Transactional
    public void deleteComment(UUID reviewId, UUID commentId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));
        ensureCanEdit(review);
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            throw new ValidationException("Cannot remove comments from a submitted review");
        }
        commentRepository.deleteByReviewIdAndId(reviewId, commentId);
    }

    // ---------- helpers ----------

    private List<CommentResponse> loadCommentResponses(UUID reviewId) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    private void ensureIsInterviewerOf(SessionAccessClient.SessionInfo info) {
        if (CurrentUser.isAdmin()) return;
        if (CurrentUser.isInterviewer() && CurrentUser.id().equals(info.getInterviewerId())) return;
        throw new ForbiddenException("Only the session interviewer can author the review");
    }

    private void ensureCanEdit(Review review) {
        if (CurrentUser.isAdmin()) return;
        if (CurrentUser.isInterviewer() && CurrentUser.id().equals(review.getReviewerId())) return;
        throw new ForbiddenException("Only the review author can edit it");
    }

    private void ensureCanView(Review review) {
        if (CurrentUser.isAdmin()) return;
        if (CurrentUser.isInterviewer() && CurrentUser.id().equals(review.getReviewerId())) return;
        if (CurrentUser.isHrManager() && review.getCompanyId().equals(CurrentUser.companyId())) return;
        if (CurrentUser.isCandidate()
                && CurrentUser.id().equals(review.getCandidateId())
                && review.getStatus() == ReviewStatus.SUBMITTED) return;
        throw new ForbiddenException("You cannot access this review");
    }
}
