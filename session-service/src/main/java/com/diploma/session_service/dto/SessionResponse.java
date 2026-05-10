package com.diploma.session_service.dto;

import com.diploma.session_service.entity.Session;
import com.diploma.session_service.entity.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String title,
        UUID companyId,
        UUID interviewerId,
        UUID candidateId,
        UUID taskId,
        String taskTitle,
        String language,
        SessionStatus status,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionResponse from(Session s) {
        return new SessionResponse(
                s.getId(),
                s.getTitle(),
                s.getCompanyId(),
                s.getInterviewerId(),
                s.getCandidateId(),
                s.getTask() != null ? s.getTask().getId() : null,
                s.getTask() != null ? s.getTask().getTitle() : null,
                s.getLanguage(),
                s.getStatus(),
                s.getScheduledAt(),
                s.getStartedAt(),
                s.getCompletedAt(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
