package com.diploma.session_service.controller;

import com.diploma.session_service.dto.TaskCategoryResponse;
import com.diploma.session_service.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/task-categories")
@RequiredArgsConstructor
public class TaskCategoryController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskCategoryResponse>> list() {
        List<TaskCategoryResponse> categories = taskService.listCategories().stream()
                .map(TaskCategoryResponse::from)
                .toList();
        return ResponseEntity.ok(categories);
    }
}
