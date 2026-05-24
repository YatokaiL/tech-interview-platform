package com.diploma.execution_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ExecutionRequest(
        @NotNull UUID sessionId,
        @NotBlank @Size(max = 50) String language,
        @NotBlank @Size(max = 100_000) String code,
        @Size(max = 65_536) String stdin
) {}
