package com.diploma.session_service.dto;

import com.diploma.session_service.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record TaskRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        String description,

        @NotNull
        Difficulty difficulty,

        @Size(max = 50)
        String language,

        String initialCode,

        String expectedOutput,

        @Positive
        Integer timeLimitMinutes,

        Set<UUID> categoryIds
) {}
