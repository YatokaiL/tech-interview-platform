package com.diploma.session_service.controller;

import com.diploma.session_service.dto.CodeSnapshotRequest;
import com.diploma.session_service.dto.CodeSnapshotResponse;
import com.diploma.session_service.dto.PageResponse;
import com.diploma.session_service.dto.SessionRequest;
import com.diploma.session_service.dto.SessionResponse;
import com.diploma.session_service.entity.CodeSnapshot;
import com.diploma.session_service.entity.Session;
import com.diploma.session_service.entity.SessionStatus;
import com.diploma.session_service.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<PageResponse<SessionResponse>> list(
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Session> sessions = sessionService.search(
                status, from, to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"))
        );
        return ResponseEntity.ok(PageResponse.from(sessions, SessionResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody SessionRequest request) {
        Session session = sessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(session));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SessionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SessionRequest request
    ) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<SessionResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.start(id)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'ADMIN')")
    public ResponseEntity<SessionResponse> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<SessionResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.cancel(id)));
    }

    @GetMapping("/{id}/snapshots/latest")
    public ResponseEntity<CodeSnapshotResponse> latestSnapshot(@PathVariable UUID id) {
        return sessionService.latestSnapshot(id)
                .map(CodeSnapshotResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/snapshots")
    public ResponseEntity<CodeSnapshotResponse> saveSnapshot(
            @PathVariable UUID id,
            @Valid @RequestBody CodeSnapshotRequest request
    ) {
        CodeSnapshot saved = sessionService.saveSnapshot(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CodeSnapshotResponse.from(saved));
    }
}
