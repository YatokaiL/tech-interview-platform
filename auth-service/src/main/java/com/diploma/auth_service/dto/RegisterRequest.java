package com.diploma.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @NotBlank
        @Pattern(regexp = "CANDIDATE|HR_MANAGER",
                 message = "role must be CANDIDATE or HR_MANAGER")
        String role,

        @Size(max = 255)
        String companyName
) {}
