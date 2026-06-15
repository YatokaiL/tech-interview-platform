package com.diploma.report_service.controller;

import com.diploma.report_service.dto.ReportRequest;
import com.diploma.report_service.dto.ReportResponse;
import com.diploma.report_service.entity.Report;
import com.diploma.report_service.entity.ReportFormat;
import com.diploma.report_service.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ReportResponse> generate(@Valid @RequestBody ReportRequest request) {
        ReportFormat format = request.format() != null ? request.format() : ReportFormat.PDF;
        Report report = reportService.generate(request.reviewId(), format);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    @GetMapping
    public ResponseEntity<List<ReportResponse>> list(@RequestParam UUID sessionId) {
        List<ReportResponse> body = reportService.listForSession(sessionId).stream()
                .map(ReportResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ReportResponse.from(reportService.getById(id)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        Report report = reportService.getById(id);
        byte[] body = reportService.download(id);

        String filename = "interview-report-" + report.getId() + "." + report.getFormat().getExtension();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(report.getFormat().getMediaType()))
                .body(body);
    }
}
