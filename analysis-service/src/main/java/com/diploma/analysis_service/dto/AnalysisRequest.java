package com.diploma.analysis_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AnalysisRequest(
        @NotNull UUID sessionId,
        @NotBlank @Size(max = 50) String language,
        @NotBlank @Size(max = 100_000) String code
) {}
