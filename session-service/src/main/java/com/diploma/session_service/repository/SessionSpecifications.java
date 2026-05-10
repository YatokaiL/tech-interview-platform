package com.diploma.session_service.repository;

import com.diploma.session_service.entity.Session;
import com.diploma.session_service.entity.SessionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class SessionSpecifications {

    private SessionSpecifications() {}

    public static Specification<Session> hasStatus(SessionStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Session> scheduledFrom(Instant from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("scheduledAt"), from);
    }

    public static Specification<Session> scheduledTo(Instant to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("scheduledAt"), to);
    }

    public static Specification<Session> hasCompany(UUID companyId) {
        if (companyId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Session> hasInterviewer(UUID interviewerId) {
        if (interviewerId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("interviewerId"), interviewerId);
    }

    public static Specification<Session> hasCandidate(UUID candidateId) {
        if (candidateId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("candidateId"), candidateId);
    }
}
