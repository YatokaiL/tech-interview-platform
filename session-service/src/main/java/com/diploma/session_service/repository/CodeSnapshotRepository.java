package com.diploma.session_service.repository;

import com.diploma.session_service.entity.CodeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, UUID> {

    Optional<CodeSnapshot> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
