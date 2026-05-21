package com.diploma.session_service.repository;

import com.diploma.session_service.entity.SessionEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionEventRepository extends JpaRepository<SessionEvent, UUID> {

    List<SessionEvent> findBySessionIdOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);

    List<SessionEvent> findBySessionIdAndEventTypeOrderByCreatedAtAsc(UUID sessionId, String eventType, Pageable pageable);
}
