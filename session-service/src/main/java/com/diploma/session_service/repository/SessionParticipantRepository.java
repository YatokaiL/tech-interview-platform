package com.diploma.session_service.repository;

import com.diploma.session_service.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {
    List<SessionParticipant> findAllBySessionId(UUID sessionId);
}
