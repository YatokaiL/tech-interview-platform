--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE reviews (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id                  UUID NOT NULL,
    reviewer_id                 UUID NOT NULL,
    candidate_id                UUID,
    company_id                  UUID NOT NULL,
    score_correctness           SMALLINT,
    score_code_quality          SMALLINT,
    score_performance           SMALLINT,
    score_communication         SMALLINT,
    verdict                     VARCHAR(50),
    summary                     TEXT,
    status                      VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    submitted_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_status   CHECK (status IN ('DRAFT', 'SUBMITTED')),
    CONSTRAINT chk_review_verdict  CHECK (verdict IS NULL OR verdict IN ('HIRE', 'NO_HIRE', 'MAYBE')),
    CONSTRAINT chk_score_correct   CHECK (score_correctness   IS NULL OR (score_correctness   BETWEEN 1 AND 5)),
    CONSTRAINT chk_score_quality   CHECK (score_code_quality  IS NULL OR (score_code_quality  BETWEEN 1 AND 5)),
    CONSTRAINT chk_score_perf      CHECK (score_performance   IS NULL OR (score_performance   BETWEEN 1 AND 5)),
    CONSTRAINT chk_score_comm      CHECK (score_communication IS NULL OR (score_communication BETWEEN 1 AND 5)),
    CONSTRAINT uq_review_per_session UNIQUE (session_id)
);

CREATE TABLE review_comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id   UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    line_number INTEGER,
    text        TEXT NOT NULL,
    author_id   UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_session    ON reviews(session_id);
CREATE INDEX idx_reviews_reviewer   ON reviews(reviewer_id);
CREATE INDEX idx_reviews_candidate  ON reviews(candidate_id);
CREATE INDEX idx_reviews_company    ON reviews(company_id);
CREATE INDEX idx_comments_review    ON review_comments(review_id);
--rollback DROP TABLE review_comments; DROP TABLE reviews;
