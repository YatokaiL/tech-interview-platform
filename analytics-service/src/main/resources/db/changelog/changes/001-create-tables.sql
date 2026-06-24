--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE analytics_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type    VARCHAR(64) NOT NULL,
    company_id    UUID,
    user_id       UUID,
    language      VARCHAR(40),
    verdict       VARCHAR(40),
    value_cents   BIGINT,
    payload       JSONB,
    occurred_at   TIMESTAMPTZ NOT NULL,
    received_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_events_type     ON analytics_events(event_type);
CREATE INDEX idx_analytics_events_occurred ON analytics_events(occurred_at);
CREATE INDEX idx_analytics_events_company  ON analytics_events(company_id);
--rollback DROP TABLE analytics_events;
