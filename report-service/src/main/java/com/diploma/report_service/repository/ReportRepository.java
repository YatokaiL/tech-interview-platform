package com.diploma.report_service.repository;

import com.diploma.report_service.entity.Report;
import com.diploma.report_service.entity.ReportFormat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    Optional<Report> findTopByReviewIdAndFormatOrderByCreatedAtDesc(UUID reviewId, ReportFormat format);
}
