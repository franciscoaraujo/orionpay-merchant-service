CREATE TABLE IF NOT EXISTS core.auth_user (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    merchant_id UUID NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT auth_user_role_check CHECK (role IN ('ROLE_MERCHANT', 'ROLE_ADMIN')),
    CONSTRAINT auth_user_merchant_fk FOREIGN KEY (merchant_id) REFERENCES core.merchant(id)
);

CREATE TABLE IF NOT EXISTS core.refresh_token (
    id UUID PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT refresh_token_user_fk FOREIGN KEY (user_id) REFERENCES core.auth_user(id)
);

CREATE INDEX IF NOT EXISTS idx_auth_user_email ON core.auth_user(email);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON core.refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON core.refresh_token(token);

