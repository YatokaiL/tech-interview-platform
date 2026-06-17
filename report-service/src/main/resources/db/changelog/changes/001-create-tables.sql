--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL,
    review_id       UUID,
    company_id      UUID NOT NULL,
    format          VARCHAR(20) NOT NULL,
    object_key      VARCHAR(500) NOT NULL,
    size_bytes      BIGINT,
    generated_by    UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_report_format CHECK (format IN ('PDF', 'JSON'))
);

CREATE INDEX idx_reports_session    ON reports(session_id, created_at DESC);
CREATE INDEX idx_reports_review     ON reports(review_id);
CREATE INDEX idx_reports_company    ON reports(company_id);
--rollback DROP TABLE reports;
