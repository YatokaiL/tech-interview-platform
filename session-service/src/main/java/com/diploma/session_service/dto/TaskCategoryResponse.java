package com.diploma.session_service.dto;

import com.diploma.session_service.entity.TaskCategory;

import java.util.UUID;

public record TaskCategoryResponse(
        UUID id,
        String name,
        String description
) {
    public static TaskCategoryResponse from(TaskCategory c) {
        return new TaskCategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
