package com.diploma.auth_service.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role,
        UUID companyId,
        Instant registeredAt
) {}
