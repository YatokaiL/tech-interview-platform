package com.diploma.execution_service.controller;

import com.diploma.execution_service.dto.ExecutionRequest;
import com.diploma.execution_service.dto.ExecutionResponse;
import com.diploma.execution_service.entity.CodeExecution;
import com.diploma.execution_service.service.ExecutionService;
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
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CANDIDATE', 'INTERVIEWER', 'ADMIN')")
    public ResponseEntity<ExecutionResponse> submit(@Valid @RequestBody ExecutionRequest request) {
        CodeExecution exec = executionService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ExecutionResponse.from(exec));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ExecutionResponse.from(executionService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> listBySession(
            @RequestParam UUID sessionId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<ExecutionResponse> body = executionService.listBySession(sessionId, limit).stream()
                .map(ExecutionResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }
}
