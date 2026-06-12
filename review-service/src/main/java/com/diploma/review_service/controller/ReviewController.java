package com.diploma.review_service.controller;

import com.diploma.review_service.dto.CommentRequest;
import com.diploma.review_service.dto.CommentResponse;
import com.diploma.review_service.dto.ReviewRequest;
import com.diploma.review_service.dto.ReviewResponse;
import com.diploma.review_service.dto.ReviewUpdateRequest;
import com.diploma.review_service.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse body = reviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/auto")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> generateAuto(@RequestParam UUID sessionId) {
        return ResponseEntity.ok(reviewService.generateAuto(sessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (sessionId != null) {
            return reviewService.findBySessionId(sessionId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        }
        List<ReviewResponse> body = reviewService.listForCurrentUser(limit);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(reviewService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<ReviewResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.submit(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResponse saved = reviewService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId
    ) {
        reviewService.deleteComment(id, commentId);
        return ResponseEntity.noContent().build();
    }
}
