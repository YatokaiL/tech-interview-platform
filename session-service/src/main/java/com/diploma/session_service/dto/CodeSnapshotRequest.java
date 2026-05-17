package com.diploma.session_service.dto;

import com.diploma.session_service.entity.SnapshotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeSnapshotRequest(
        @NotBlank @Size(max = 50) String language,
        @NotBlank String code,
        SnapshotType snapshotType
) {}
