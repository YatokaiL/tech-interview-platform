package com.diploma.analysis_service.repository;

import com.diploma.analysis_service.entity.CodeAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeAnalysisRepository extends JpaRepository<CodeAnalysis, UUID> {

    List<CodeAnalysis> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);
}
