package com.diploma.review_service.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class CurrentUser {

    public static final String ROLE_CANDIDATE   = "CANDIDATE";
    public static final String ROLE_INTERVIEWER = "INTERVIEWER";
    public static final String ROLE_HR_MANAGER  = "HR_MANAGER";
    public static final String ROLE_ADMIN       = "ADMIN";

    private CurrentUser() {}

    public static Jwt jwt() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) return jwt;
        throw new IllegalStateException("No JWT principal in security context");
    }

    public static UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public static String role() {
        return jwt().getClaimAsString("role");
    }

    public static UUID companyId() {
        String value = jwt().getClaimAsString("companyId");
        return value == null ? null : UUID.fromString(value);
    }

    public static String rawToken() {
        return jwt().getTokenValue();
    }

    public static boolean isAdmin()       { return ROLE_ADMIN.equals(role()); }
    public static boolean isInterviewer() { return ROLE_INTERVIEWER.equals(role()); }
    public static boolean isHrManager()   { return ROLE_HR_MANAGER.equals(role()); }
    public static boolean isCandidate()   { return ROLE_CANDIDATE.equals(role()); }
}
