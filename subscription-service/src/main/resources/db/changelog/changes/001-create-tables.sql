--liquibase formatted sql

--changeset diploma:001-create-tables splitStatements:true endDelimiter:;
CREATE TABLE tariffs (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                        VARCHAR(50) NOT NULL UNIQUE,
    name                        VARCHAR(100) NOT NULL,
    description                 TEXT,
    price_cents                 INTEGER NOT NULL,
    currency                    VARCHAR(8) NOT NULL DEFAULT 'RUB',
    max_sessions_per_month      INTEGER,
    features                    TEXT,
    sort_order                  INTEGER NOT NULL DEFAULT 0,
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL,
    tariff_id       UUID NOT NULL REFERENCES tariffs(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    period_months   INTEGER NOT NULL DEFAULT 1,
    started_at      TIMESTAMPTZ,
    ends_at         TIMESTAMPTZ,
    auto_renew      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_subscription_status CHECK (status IN ('PENDING','ACTIVE','EXPIRED','CANCELLED'))
);

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    amount_cents    INTEGER NOT NULL,
    currency        VARCHAR(8) NOT NULL DEFAULT 'RUB',
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    gateway_ref     VARCHAR(100),
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING','SUCCEEDED','FAILED','REFUNDED'))
);

CREATE INDEX idx_subscriptions_company ON subscriptions(company_id, created_at DESC);
CREATE INDEX idx_subscriptions_status  ON subscriptions(status);
CREATE INDEX idx_payments_subscription ON payments(subscription_id);
--rollback DROP TABLE payments; DROP TABLE subscriptions; DROP TABLE tariffs;
