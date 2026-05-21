package com.diploma.session_service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionEventRequest(
        @NotBlank @Size(max = 100) String eventType,
        JsonNode payload
) {}
