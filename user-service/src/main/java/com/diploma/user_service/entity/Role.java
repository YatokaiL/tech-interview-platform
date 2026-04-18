package com.diploma.user_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    public static final String CANDIDATE   = "CANDIDATE";
    public static final String INTERVIEWER = "INTERVIEWER";
    public static final String HR_MANAGER  = "HR_MANAGER";
    public static final String ADMIN       = "ADMIN";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
