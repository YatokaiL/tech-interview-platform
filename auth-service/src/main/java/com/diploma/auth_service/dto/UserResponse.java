package com.diploma.auth_service.dto;

import com.diploma.auth_service.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        UUID companyId,
        boolean blocked,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName(),
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.isBlocked(),
                user.getCreatedAt()
        );
    }
}
