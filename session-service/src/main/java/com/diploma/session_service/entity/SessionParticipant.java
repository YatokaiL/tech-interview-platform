package com.diploma.session_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_participants")
@Getter
@Setter
@NoArgsConstructor
public class SessionParticipant {

    public static final String ROLE_INTERVIEWER = "INTERVIEWER";
    public static final String ROLE_CANDIDATE   = "CANDIDATE";

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
