package com.diploma.user_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Pattern(regexp = "CANDIDATE|INTERVIEWER|HR_MANAGER|ADMIN",
                 message = "role must be one of CANDIDATE, INTERVIEWER, HR_MANAGER, ADMIN")
        String role,

        Boolean blocked
) {}
