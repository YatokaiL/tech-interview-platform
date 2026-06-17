package com.diploma.report_service.service;

import com.diploma.report_service.client.ReviewClient;
import com.diploma.report_service.client.SessionAccessClient;
import com.diploma.report_service.client.SnapshotClient;
import com.diploma.report_service.client.UserClient;
import com.diploma.report_service.entity.Report;
import com.diploma.report_service.entity.ReportFormat;
import com.diploma.report_service.exception.ForbiddenException;
import com.diploma.report_service.exception.NotFoundException;
import com.diploma.report_service.exception.ValidationException;
import com.diploma.report_service.repository.ReportRepository;
import com.diploma.report_service.security.CurrentUser;
import com.diploma.report_service.storage.MinioStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository repository;
    private final SessionAccessClient sessionClient;
    private final ReviewClient reviewClient;
    private final SnapshotClient snapshotClient;
    private final UserClient userClient;
    private final PdfGenerator pdfGenerator;
    private final JsonExporter jsonExporter;
    private final MinioStorage minio;

    @Transactional
    public Report generate(UUID reviewId, ReportFormat format) {
        String token = CurrentUser.rawToken();
        ReviewClient.ReviewInfo review = reviewClient.fetch(reviewId, token);
        if (review == null) throw new NotFoundException("Review", reviewId);
        if (!"SUBMITTED".equalsIgnoreCase(review.getStatus())) {
            throw new ValidationException("Report can be generated only for a SUBMITTED review");
        }

        SessionAccessClient.SessionInfo session = sessionClient.fetchAccessible(review.getSessionId(), token);
        if (session == null) {
            throw new ForbiddenException("Session is not accessible to current user");
        }
        ensureCanGenerate(review);

        SnapshotClient.SnapshotInfo snapshot = snapshotClient.fetchLatest(session.getId(), token);
        UserClient.UserInfo interviewer = userClient.fetch(session.getInterviewerId(), token);
        UserClient.UserInfo candidate = userClient.fetch(session.getCandidateId(), token);

        ReportContext ctx = new ReportContext(session, review, snapshot, interviewer, candidate);

        byte[] body = switch (format) {
            case PDF -> pdfGenerator.render(ctx);
            case JSON -> jsonExporter.render(ctx);
        };

        String key = objectKey(session, review, format);
        minio.upload(key, body, format.getMediaType());

        Report report = new Report();
        report.setSessionId(session.getId());
        report.setReviewId(review.getId());
        report.setCompanyId(session.getCompanyId());
        report.setFormat(format);
        report.setObjectKey(key);
        report.setSizeBytes((long) body.length);
        report.setGeneratedBy(CurrentUser.id());
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public Report getById(UUID id) {
        Report report = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report", id));
        ensureCanView(report);
        return report;
    }

    @Transactional(readOnly = true)
    public Optional<Report> findLatestForReview(UUID reviewId, ReportFormat format) {
        return repository.findTopByReviewIdAndFormatOrderByCreatedAtDesc(reviewId, format);
    }

    @Transactional(readOnly = true)
    public List<Report> listForSession(UUID sessionId) {
        SessionAccessClient.SessionInfo info = sessionClient.fetchAccessible(sessionId, CurrentUser.rawToken());
        if (info == null) throw new ForbiddenException("Session is not accessible to current user");
        return repository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    public byte[] download(UUID id) {
        Report report = getById(id);
        return minio.downloadBytes(report.getObjectKey());
    }

    private void ensureCanGenerate(ReviewClient.ReviewInfo review) {
        if (CurrentUser.isAdmin()) return;
        if (CurrentUser.isInterviewer() && CurrentUser.id().equals(review.getReviewerId())) return;
        if (CurrentUser.isHrManager() && review.getCompanyId() != null
                && review.getCompanyId().equals(CurrentUser.companyId())) return;
        throw new ForbiddenException("Only the review author, HR of the company or admin can generate a report");
    }

    private void ensureCanView(Report report) {
        if (CurrentUser.isAdmin()) return;
        if (CurrentUser.isHrManager() && report.getCompanyId().equals(CurrentUser.companyId())) return;
        SessionAccessClient.SessionInfo info = sessionClient.fetchAccessible(report.getSessionId(), CurrentUser.rawToken());
        if (info == null) {
            throw new ForbiddenException("Report is not accessible to current user");
        }
        // Candidate of the session may also view their own submitted report — the session-service
        // access check has already validated that the user is allowed to see the session row.
    }

    private String objectKey(SessionAccessClient.SessionInfo session, ReviewClient.ReviewInfo review, ReportFormat format) {
        return String.format("%s/%s/review-%s-%d.%s",
                session.getCompanyId(),
                session.getId(),
                review.getId(),
                System.currentTimeMillis(),
                format.getExtension());
    }
}
