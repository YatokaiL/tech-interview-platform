package com.diploma.session_service.dto;

import com.diploma.session_service.entity.CodeSnapshot;
import com.diploma.session_service.entity.SnapshotType;

import java.time.Instant;
import java.util.UUID;

public record CodeSnapshotResponse(
        UUID id,
        UUID sessionId,
        String language,
        String code,
        SnapshotType snapshotType,
        Instant createdAt
) {
    public static CodeSnapshotResponse from(CodeSnapshot s) {
        return new CodeSnapshotResponse(
                s.getId(),
                s.getSessionId(),
                s.getLanguage(),
                s.getCode(),
                s.getSnapshotType(),
                s.getCreatedAt()
        );
    }
}
