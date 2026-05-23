--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE code_executions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL,
    user_id         UUID NOT NULL,
    language        VARCHAR(50) NOT NULL,
    code            TEXT NOT NULL,
    stdin           TEXT,
    stdout          TEXT,
    stderr          TEXT,
    exit_code       INTEGER,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    duration_ms     BIGINT,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_exec_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILURE', 'TIMEOUT', 'ERROR'))
);

CREATE INDEX idx_code_executions_session ON code_executions(session_id, created_at DESC);
CREATE INDEX idx_code_executions_user    ON code_executions(user_id, created_at DESC);
--rollback DROP TABLE code_executions;
