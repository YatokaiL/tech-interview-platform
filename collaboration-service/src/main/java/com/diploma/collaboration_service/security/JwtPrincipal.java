package com.diploma.collaboration_service.security;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String email,
        String role,
        UUID companyId,
        String rawToken
) {
    public boolean isInterviewer() { return "INTERVIEWER".equals(role); }
    public boolean isCandidate()   { return "CANDIDATE".equals(role); }
    public boolean isHrManager()   { return "HR_MANAGER".equals(role); }
    public boolean isAdmin()       { return "ADMIN".equals(role); }
}
