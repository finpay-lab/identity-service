-- FP-30/63: identity schema. No shared DB (Rule 1).
CREATE TABLE IF NOT EXISTS kyc_verifications (
    customer_id    VARCHAR(36) PRIMARY KEY,
    state          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    full_name      VARCHAR(255),
    date_of_birth  VARCHAR(32),
    document_number VARCHAR(64),
    expiry         VARCHAR(32),
    created_at     TIMESTAMP   NOT NULL,
    reviewed_at    TIMESTAMP
);
