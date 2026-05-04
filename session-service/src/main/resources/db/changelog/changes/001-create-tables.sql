--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE task_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tasks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,
    difficulty          VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    language            VARCHAR(50),
    initial_code        TEXT,
    expected_output     TEXT,
    time_limit_minutes  INTEGER,
    company_id          UUID,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE TABLE task_categories_link (
    task_id     UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES task_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, category_id)
);

CREATE TABLE sessions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title          VARCHAR(255) NOT NULL,
    company_id     UUID NOT NULL,
    interviewer_id UUID NOT NULL,
    candidate_id   UUID,
    task_id        UUID REFERENCES tasks(id),
    language       VARCHAR(50) NOT NULL,
    status         VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at   TIMESTAMPTZ NOT NULL,
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_session_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE session_participants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    role        VARCHAR(50) NOT NULL,
    joined_at   TIMESTAMPTZ,
    left_at     TIMESTAMPTZ,
    CONSTRAINT chk_participant_role CHECK (role IN ('INTERVIEWER', 'CANDIDATE'))
);

CREATE TABLE code_snapshots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    code          TEXT NOT NULL,
    language      VARCHAR(50) NOT NULL,
    snapshot_type VARCHAR(50) NOT NULL DEFAULT 'AUTO',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_snapshot_type CHECK (snapshot_type IN ('AUTO', 'MANUAL', 'FINAL'))
);

CREATE TABLE session_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB,
    user_id     UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_company       ON sessions(company_id);
CREATE INDEX idx_sessions_interviewer   ON sessions(interviewer_id);
CREATE INDEX idx_sessions_candidate     ON sessions(candidate_id);
CREATE INDEX idx_sessions_status        ON sessions(status);
CREATE INDEX idx_session_events_session ON session_events(session_id);
CREATE INDEX idx_code_snapshots_session ON code_snapshots(session_id);
CREATE INDEX idx_tasks_company          ON tasks(company_id);
CREATE INDEX idx_tasks_created_by       ON tasks(created_by);
--rollback DROP TABLE session_events; DROP TABLE code_snapshots; DROP TABLE session_participants; DROP TABLE sessions; DROP TABLE task_categories_link; DROP TABLE tasks; DROP TABLE task_categories;
