package com.diploma.user_service.dto;

import com.diploma.user_service.entity.Company;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
