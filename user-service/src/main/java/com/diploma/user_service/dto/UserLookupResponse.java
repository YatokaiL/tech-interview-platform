package com.diploma.user_service.dto;

import com.diploma.user_service.entity.User;

import java.util.UUID;

public record UserLookupResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role
) {
    public static UserLookupResponse from(User user) {
        return new UserLookupResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName()
        );
    }
}
