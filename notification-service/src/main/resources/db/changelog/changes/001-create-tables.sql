--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID NOT NULL,
    recipient_email     VARCHAR(255),
    type                VARCHAR(50) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    message             TEXT NOT NULL,
    link                VARCHAR(500),
    email_sent          BOOLEAN NOT NULL DEFAULT FALSE,
    read                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_unread    ON notifications(recipient_user_id, read);
--rollback DROP TABLE notifications;
