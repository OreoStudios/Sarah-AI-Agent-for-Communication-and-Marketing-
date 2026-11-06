-- Conversational (passwordless, email-OTP) authentication.
-- Accounts registered through Sarah's chat. The seeded owner (app_user) keeps HTTP Basic.

CREATE TABLE user_account (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING / ACTIVE / LOCKED
    role          VARCHAR(32)  NOT NULL DEFAULT 'OWNER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    verified_at   TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ
);

-- One live challenge per email (older ones are deleted when a new code is requested).
CREATE TABLE auth_challenge (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    code_hmac     VARCHAR(128) NOT NULL,                    -- HMAC-SHA256(secret, email:code); plaintext never stored
    state         VARCHAR(16)  NOT NULL DEFAULT 'SENT',     -- SENT / CONSUMED / LOCKED
    attempts      INT          NOT NULL DEFAULT 0,
    max_attempts  INT          NOT NULL DEFAULT 5,
    resend_count  INT          NOT NULL DEFAULT 0,
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_sent_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    request_ip    VARCHAR(64)
);
CREATE INDEX idx_auth_challenge_email ON auth_challenge(email);
CREATE INDEX idx_auth_challenge_expires ON auth_challenge(expires_at);
