-- ADR-0005: identity-service owns this schema. OIDC-mapped internal principals
-- plus their role grants (ADR-0006). `(identity_provider, external_subject)` is
-- the idempotency backstop for concurrent logins (Rule 6).
CREATE TABLE principals (
    principal_id      UUID PRIMARY KEY,
    external_subject  VARCHAR(256) NOT NULL,
    identity_provider VARCHAR(128) NOT NULL,
    email             VARCHAR(256),
    display_name      VARCHAR(256),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_principals_idp_subject UNIQUE (identity_provider, external_subject)
);

CREATE INDEX idx_principals_external_subject ON principals (external_subject);

-- Principal role grants (stored role identifiers from com.finpay.common.security.Role).
CREATE TABLE principal_roles (
    principal_id UUID        NOT NULL REFERENCES principals (principal_id) ON DELETE CASCADE,
    role         VARCHAR(32) NOT NULL,
    PRIMARY KEY (principal_id, role)
);