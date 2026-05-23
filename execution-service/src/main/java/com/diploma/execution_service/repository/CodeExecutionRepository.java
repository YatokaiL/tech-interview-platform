package com.diploma.execution_service.repository;

import com.diploma.execution_service.entity.CodeExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeExecutionRepository extends JpaRepository<CodeExecution, UUID> {

    List<CodeExecution> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);
}
