-- Sarah AI Agent — initial schema
-- Postgres + pgvector. All timestamps are timestamptz (UTC).

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Security
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Brand profile (configurable, not hardcoded in prompts) — single active row
-- ---------------------------------------------------------------------------
CREATE TABLE brand_profile (
    id            UUID PRIMARY KEY,
    company_name  VARCHAR(255) NOT NULL,
    description   TEXT         NOT NULL,
    services      TEXT         NOT NULL,          -- newline-separated services
    tone          VARCHAR(255) NOT NULL,
    languages     VARCHAR(255) NOT NULL,          -- csv, e.g. "en,fr"
    website       VARCHAR(500),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Autonomy settings (persisted kill switch + limits) — single row id=1 pattern
-- ---------------------------------------------------------------------------
CREATE TABLE autonomy_settings (
    id                            UUID PRIMARY KEY,
    enabled                       BOOLEAN NOT NULL DEFAULT TRUE,
    daily_post_limit              INT     NOT NULL DEFAULT 10,
    per_platform_cooldown_minutes INT     NOT NULL DEFAULT 30,
    max_llm_calls_per_run         INT     NOT NULL DEFAULT 25,
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Campaigns + workflow
-- ---------------------------------------------------------------------------
CREATE TABLE campaign (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,     -- prevents duplicate campaigns
    objective       TEXT         NOT NULL,
    target_audience VARCHAR(500) NOT NULL,
    platforms       VARCHAR(500) NOT NULL,            -- csv of platform codes
    language        VARCHAR(8)   NOT NULL DEFAULT 'en',
    status          VARCHAR(32)  NOT NULL,            -- workflow state
    budget_cents    BIGINT       NOT NULL DEFAULT 0,
    start_at        TIMESTAMPTZ,
    end_at          TIMESTAMPTZ,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE workflow_task (
    id            UUID PRIMARY KEY,
    campaign_id   UUID REFERENCES campaign(id) ON DELETE CASCADE,
    type          VARCHAR(64)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    agent         VARCHAR(64),
    payload       TEXT,
    attempts      INT          NOT NULL DEFAULT 0,
    max_attempts  INT          NOT NULL DEFAULT 3,
    last_error    TEXT,
    scheduled_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_workflow_task_campaign ON workflow_task(campaign_id);
CREATE INDEX idx_workflow_task_status   ON workflow_task(status);

-- ---------------------------------------------------------------------------
-- Content drafts
-- ---------------------------------------------------------------------------
CREATE TABLE content_draft (
    id                UUID PRIMARY KEY,
    campaign_id       UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    platform          VARCHAR(32)  NOT NULL,
    language          VARCHAR(8)   NOT NULL DEFAULT 'en',
    title             VARCHAR(500),
    body              TEXT         NOT NULL,
    image_brief       TEXT,
    status            VARCHAR(32)  NOT NULL,          -- DRAFT/APPROVED/REJECTED
    compliance_score  INT,
    compliance_notes  TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_content_draft_campaign ON content_draft(campaign_id);

-- ---------------------------------------------------------------------------
-- Connector accounts (channels). Tokens are encrypted at rest (app-side AES).
-- ---------------------------------------------------------------------------
CREATE TABLE connector_account (
    id              UUID PRIMARY KEY,
    type            VARCHAR(32)  NOT NULL,            -- DISCORD/LINKEDIN/...
    name            VARCHAR(255) NOT NULL,
    config          TEXT,                             -- JSON (non-secret config)
    encrypted_token TEXT,                             -- AES-GCM, base64
    capabilities    VARCHAR(255) NOT NULL DEFAULT '', -- csv of capabilities
    status          VARCHAR(32)  NOT NULL,            -- CONNECTED/DISCONNECTED/UNSUPPORTED
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (type, name)
);

-- ---------------------------------------------------------------------------
-- Publications (content calendar + publishing history)
-- ---------------------------------------------------------------------------
CREATE TABLE publication (
    id                    UUID PRIMARY KEY,
    campaign_id           UUID REFERENCES campaign(id) ON DELETE SET NULL,
    draft_id              UUID REFERENCES content_draft(id) ON DELETE SET NULL,
    connector_account_id  UUID NOT NULL REFERENCES connector_account(id),
    platform              VARCHAR(32)  NOT NULL,
    content               TEXT         NOT NULL,
    scheduled_at          TIMESTAMPTZ  NOT NULL,
    status                VARCHAR(32)  NOT NULL,       -- SCHEDULED/PUBLISHING/PUBLISHED/FAILED/CANCELLED
    attempts              INT          NOT NULL DEFAULT 0,
    max_attempts          INT          NOT NULL DEFAULT 3,
    idempotency_key       VARCHAR(255) NOT NULL UNIQUE,
    external_id           VARCHAR(255),
    external_url          VARCHAR(1000),
    last_error            TEXT,
    published_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_publication_status_sched ON publication(status, scheduled_at);
CREATE INDEX idx_publication_campaign     ON publication(campaign_id);

-- ---------------------------------------------------------------------------
-- Audit log (append-only)
-- ---------------------------------------------------------------------------
CREATE TABLE audit_log (
    id           UUID PRIMARY KEY,
    actor        VARCHAR(255) NOT NULL,
    action       VARCHAR(128) NOT NULL,
    entity_type  VARCHAR(64),
    entity_id    VARCHAR(64),
    detail       TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_log(created_at);

-- ---------------------------------------------------------------------------
-- Prospecting CRM
-- ---------------------------------------------------------------------------
CREATE TABLE prospect (
    id             UUID PRIMARY KEY,
    business_name  VARCHAR(255) NOT NULL,
    website        VARCHAR(500),
    sector         VARCHAR(255),
    location       VARCHAR(255),
    lead_score     INT          NOT NULL DEFAULT 0,
    contact_status VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    notes          TEXT,
    outreach_draft TEXT,
    suppressed     BOOLEAN      NOT NULL DEFAULT FALSE,  -- opt-out / suppression
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_prospect_status ON prospect(contact_status);

-- ---------------------------------------------------------------------------
-- Conversation memory (long-term)
-- ---------------------------------------------------------------------------
CREATE TABLE chat_message (
    id              UUID PRIMARY KEY,
    conversation_id UUID         NOT NULL,
    role            VARCHAR(16)  NOT NULL,            -- USER/ASSISTANT/SYSTEM
    content         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_conversation ON chat_message(conversation_id, created_at);

-- ---------------------------------------------------------------------------
-- Semantic memory (pgvector). Managed via native SQL (no JPA mapping).
-- 1536-dim default; embeddings populated only when an LLM provider is configured.
-- ---------------------------------------------------------------------------
CREATE TABLE semantic_memory (
    id          UUID PRIMARY KEY,
    kind        VARCHAR(64)  NOT NULL,
    ref_id      VARCHAR(64),
    content     TEXT         NOT NULL,
    embedding   vector(1536),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
