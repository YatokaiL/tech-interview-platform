package com.diploma.session_service.dto;

import com.diploma.session_service.entity.Difficulty;
import com.diploma.session_service.entity.Task;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        Difficulty difficulty,
        String language,
        String initialCode,
        String expectedOutput,
        Integer timeLimitMinutes,
        UUID companyId,
        UUID createdBy,
        List<TaskCategoryResponse> categories,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getDifficulty(),
                t.getLanguage(),
                t.getInitialCode(),
                t.getExpectedOutput(),
                t.getTimeLimitMinutes(),
                t.getCompanyId(),
                t.getCreatedBy(),
                t.getCategories().stream().map(TaskCategoryResponse::from).toList(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
