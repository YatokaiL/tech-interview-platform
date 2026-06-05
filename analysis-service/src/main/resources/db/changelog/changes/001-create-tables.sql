--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE code_analyses (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id              UUID NOT NULL,
    user_id                 UUID NOT NULL,
    language                VARCHAR(50) NOT NULL,
    code                    TEXT NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    lines_of_code           INTEGER,
    cyclomatic_complexity   INTEGER,
    cognitive_complexity    INTEGER,
    method_count            INTEGER,
    issues                  JSONB,
    duration_ms             BIGINT,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_analysis_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'ERROR'))
);

CREATE INDEX idx_code_analyses_session ON code_analyses(session_id, created_at DESC);
CREATE INDEX idx_code_analyses_user    ON code_analyses(user_id, created_at DESC);
--rollback DROP TABLE code_analyses;
