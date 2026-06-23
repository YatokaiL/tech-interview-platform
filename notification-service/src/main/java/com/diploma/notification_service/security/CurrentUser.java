package com.diploma.notification_service.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {}

    public static Jwt jwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) return jwt;
        throw new IllegalStateException("No JWT principal in security context");
    }

    public static UUID id() {
        return UUID.fromString(jwt().getSubject());
    }
}
