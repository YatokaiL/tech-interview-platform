package com.diploma.session_service.controller;

import com.diploma.session_service.dto.PageResponse;
import com.diploma.session_service.dto.TaskRequest;
import com.diploma.session_service.dto.TaskResponse;
import com.diploma.session_service.entity.Difficulty;
import com.diploma.session_service.entity.Task;
import com.diploma.session_service.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponse<TaskResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Task> tasks = taskService.search(
                q, difficulty, language, categoryId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(PageResponse.from(tasks, TaskResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(TaskResponse.from(taskService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        Task task = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request
    ) {
        return ResponseEntity.ok(TaskResponse.from(taskService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INTERVIEWER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
