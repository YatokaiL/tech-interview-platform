package com.diploma.session_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record SessionRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        UUID candidateId,

        UUID interviewerId,

        UUID taskId,

        @NotBlank
        @Size(max = 50)
        String language,

        @NotNull
        Instant scheduledAt
) {}
