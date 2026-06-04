package com.diploma.analysis_service.controller;

import com.diploma.analysis_service.dto.AnalysisRequest;
import com.diploma.analysis_service.dto.AnalysisResponse;
import com.diploma.analysis_service.entity.CodeAnalysis;
import com.diploma.analysis_service.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'INTERVIEWER', 'ADMIN')")
    public ResponseEntity<AnalysisResponse> submit(@Valid @RequestBody AnalysisRequest request) {
        CodeAnalysis analysis = analysisService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AnalysisResponse.from(analysis));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(AnalysisResponse.from(analysisService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<List<AnalysisResponse>> listBySession(
            @RequestParam UUID sessionId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<AnalysisResponse> body = analysisService.listBySession(sessionId, limit).stream()
                .map(AnalysisResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }
}
