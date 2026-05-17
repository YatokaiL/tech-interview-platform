package com.diploma.session_service.service;

import com.diploma.session_service.dto.CodeSnapshotRequest;
import com.diploma.session_service.dto.SessionCompletedEvent;
import com.diploma.session_service.dto.SessionCreatedEvent;
import com.diploma.session_service.dto.SessionRequest;
import com.diploma.session_service.entity.CodeSnapshot;
import com.diploma.session_service.entity.Session;
import com.diploma.session_service.entity.SessionParticipant;
import com.diploma.session_service.entity.SessionStatus;
import com.diploma.session_service.entity.SnapshotType;
import com.diploma.session_service.entity.Task;
import com.diploma.session_service.exception.ForbiddenException;
import com.diploma.session_service.exception.NotFoundException;
import com.diploma.session_service.exception.ValidationException;
import com.diploma.session_service.kafka.SessionEventPublisher;
import com.diploma.session_service.repository.CodeSnapshotRepository;
import com.diploma.session_service.repository.SessionParticipantRepository;
import com.diploma.session_service.repository.SessionRepository;
import com.diploma.session_service.repository.SessionSpecifications;
import com.diploma.session_service.repository.TaskRepository;
import com.diploma.session_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final TaskRepository taskRepository;
    private final CodeSnapshotRepository snapshotRepository;
    private final SessionEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Session> search(SessionStatus status, Instant from, Instant to, Pageable pageable) {
        Specification<Session> visibility = visibilityScope();
        Specification<Session> spec = Specification.allOf(
                visibility,
                SessionSpecifications.hasStatus(status),
                SessionSpecifications.scheduledFrom(from),
                SessionSpecifications.scheduledTo(to)
        );
        return sessionRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Session getById(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session", id));
        ensureCanView(session);
        return session;
    }

    @Transactional
    public Session create(SessionRequest request) {
        if (!CurrentUser.isInterviewer() && !CurrentUser.isHrManager() && !CurrentUser.isAdmin()) {
            throw new ForbiddenException("Only INTERVIEWER, HR_MANAGER or ADMIN can create sessions");
        }

        UUID companyId = CurrentUser.companyId();
        if (companyId == null && !CurrentUser.isAdmin()) {
            throw new ValidationException("Current user has no company; cannot create session");
        }

        UUID interviewerId = resolveInterviewerId(request);

        Session session = new Session();
        session.setTitle(request.title());
        session.setCompanyId(companyId);
        session.setInterviewerId(interviewerId);
        session.setCandidateId(request.candidateId());
        session.setLanguage(request.language());
        session.setScheduledAt(request.scheduledAt());

        if (request.taskId() != null) {
            Task task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new NotFoundException("Task", request.taskId()));
            session.setTask(task);
        }

        session = sessionRepository.save(session);

        SessionParticipant interviewer = new SessionParticipant();
        interviewer.setSessionId(session.getId());
        interviewer.setUserId(interviewerId);
        interviewer.setRole(SessionParticipant.ROLE_INTERVIEWER);
        participantRepository.save(interviewer);

        if (request.candidateId() != null) {
            SessionParticipant candidate = new SessionParticipant();
            candidate.setSessionId(session.getId());
            candidate.setUserId(request.candidateId());
            candidate.setRole(SessionParticipant.ROLE_CANDIDATE);
            participantRepository.save(candidate);
        }

        eventPublisher.publishSessionCreated(new SessionCreatedEvent(
                session.getId(),
                session.getCompanyId(),
                session.getInterviewerId(),
                session.getCandidateId(),
                session.getTask() != null ? session.getTask().getId() : null,
                session.getLanguage(),
                session.getScheduledAt(),
                session.getCreatedAt()
        ));

        log.info("Created session {} (company={}, interviewer={}, candidate={})",
                session.getId(), session.getCompanyId(), session.getInterviewerId(), session.getCandidateId());
        return session;
    }

    @Transactional
    public Session update(UUID id, SessionRequest request) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session", id));
        ensureCanEdit(session);

        session.setTitle(request.title());
        session.setLanguage(request.language());
        session.setScheduledAt(request.scheduledAt());
        session.setCandidateId(request.candidateId());

        if (request.taskId() != null) {
            Task task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new NotFoundException("Task", request.taskId()));
            session.setTask(task);
        } else {
            session.setTask(null);
        }
        return session;
    }

    @Transactional
    public void delete(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session", id));
        ensureCanEdit(session);
        sessionRepository.delete(session);
    }

    @Transactional
    public Session start(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session", id));
        ensureInterviewerOf(session);
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new ValidationException("Only SCHEDULED sessions can be started");
        }
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        return session;
    }

    @Transactional
    public Session complete(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session", id));
        ensureInterviewerOf(session);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ValidationException("Only IN_PROGRESS sessions can be completed");
        }
        Instant completedAt = Instant.now();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(completedAt);

        eventPublisher.publishSessionCompleted(new SessionCompletedEvent(
                session.getId(),
                session.getCompanyId(),
                session.getInterviewerId(),
                session.getCandidateId(),
                completedAt
        ));
        return session;
    }

    @Transactional
    public Session cancel(UUID id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Session", id));
        ensureCanEdit(session);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new ValidationException("Completed sessions cannot be cancelled");
        }
        session.setStatus(SessionStatus.CANCELLED);
        return session;
    }

    // --- visibility / authorization helpers --------------------------------

    private Specification<Session> visibilityScope() {
        if (CurrentUser.isAdmin()) return null;
        UUID userId = CurrentUser.id();

        if (CurrentUser.isInterviewer()) {
            return SessionSpecifications.hasInterviewer(userId);
        }
        if (CurrentUser.isCandidate()) {
            return SessionSpecifications.hasCandidate(userId);
        }
        if (CurrentUser.isHrManager()) {
            UUID companyId = CurrentUser.companyId();
            if (companyId == null) {
                throw new ForbiddenException("HR manager without companyId cannot view sessions");
            }
            return SessionSpecifications.hasCompany(companyId);
        }
        return SessionSpecifications.hasInterviewer(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    private void ensureCanView(Session session) {
        if (CurrentUser.isAdmin()) return;
        UUID userId = CurrentUser.id();

        if (CurrentUser.isInterviewer() && session.getInterviewerId().equals(userId)) return;
        if (CurrentUser.isCandidate() && userId.equals(session.getCandidateId())) return;
        if (CurrentUser.isHrManager() && session.getCompanyId().equals(CurrentUser.companyId())) return;

        throw new ForbiddenException("You cannot access this session");
    }

    private void ensureCanEdit(Session session) {
        if (CurrentUser.isAdmin()) return;
        UUID userId = CurrentUser.id();

        if (CurrentUser.isInterviewer() && session.getInterviewerId().equals(userId)) return;
        if (CurrentUser.isHrManager() && session.getCompanyId().equals(CurrentUser.companyId())) return;

        throw new ForbiddenException("You cannot modify this session");
    }

    private void ensureInterviewerOf(Session session) {
        if (CurrentUser.isAdmin()) return;
        if (!CurrentUser.isInterviewer() || !session.getInterviewerId().equals(CurrentUser.id())) {
            throw new ForbiddenException("Only the assigned interviewer can perform this action");
        }
    }

    // --- code snapshots ----------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<CodeSnapshot> latestSnapshot(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session", sessionId));
        ensureCanView(session);
        return snapshotRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    @Transactional
    public CodeSnapshot saveSnapshot(UUID sessionId, CodeSnapshotRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session", sessionId));
        ensureCanEditCode(session);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ValidationException("Cannot save snapshot for a cancelled session");
        }

        CodeSnapshot snapshot = new CodeSnapshot();
        snapshot.setSessionId(sessionId);
        snapshot.setLanguage(request.language());
        snapshot.setCode(request.code());
        snapshot.setSnapshotType(request.snapshotType() != null ? request.snapshotType() : SnapshotType.AUTO);
        return snapshotRepository.save(snapshot);
    }

    private void ensureCanEditCode(Session session) {
        if (CurrentUser.isAdmin()) return;
        UUID userId = CurrentUser.id();
        if (CurrentUser.isInterviewer() && session.getInterviewerId().equals(userId)) return;
        if (CurrentUser.isCandidate() && userId.equals(session.getCandidateId())) return;
        throw new ForbiddenException("Only the assigned interviewer or candidate can edit the session code");
    }

    private UUID resolveInterviewerId(SessionRequest request) {
        if (CurrentUser.isInterviewer()) {
            return CurrentUser.id();
        }
        if (request.interviewerId() == null) {
            throw new ValidationException("interviewerId is required when caller is not an interviewer");
        }
        return request.interviewerId();
    }
}
