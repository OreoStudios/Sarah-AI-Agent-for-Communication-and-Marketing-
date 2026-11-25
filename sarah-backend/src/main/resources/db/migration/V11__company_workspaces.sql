-- SARAH SaaS workspaces: existing data is quarantined, never auto-exposed to new signups.
-- Flyway runs as the schema owner. Application web requests SET LOCAL ROLE to the
-- non-superuser sarah_workspace_runtime and SET LOCAL sarah.workspace_id.
CREATE TABLE workspace (
  id UUID PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  website VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE workspace_membership (
  workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
  email VARCHAR(320) NOT NULL,
  role VARCHAR(16) NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER','VIEWER')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, email),
  CONSTRAINT workspace_membership_normalized_email CHECK (email = lower(email))
);
CREATE INDEX idx_workspace_membership_email ON workspace_membership(email);
INSERT INTO workspace(id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Legacy data — unclaimed');

-- One-time backfill protects existing single-company data, which stays quarantined.
ALTER TABLE brand_profile ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE brand_profile ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_brand_profile_workspace ON brand_profile(workspace_id);
ALTER TABLE autonomy_settings ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE autonomy_settings ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_autonomy_settings_workspace ON autonomy_settings(workspace_id);
ALTER TABLE campaign ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE campaign ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_campaign_workspace ON campaign(workspace_id);
ALTER TABLE workflow_task ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE workflow_task ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_workflow_task_workspace ON workflow_task(workspace_id);
ALTER TABLE content_draft ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE content_draft ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_content_draft_workspace ON content_draft(workspace_id);
ALTER TABLE connector_account ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE connector_account ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_connector_account_workspace ON connector_account(workspace_id);
ALTER TABLE publication ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE publication ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_publication_workspace ON publication(workspace_id);
ALTER TABLE audit_log ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE audit_log ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_audit_log_workspace ON audit_log(workspace_id);
ALTER TABLE prospect ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE prospect ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_prospect_workspace ON prospect(workspace_id);
ALTER TABLE chat_message ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE chat_message ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_chat_message_workspace ON chat_message(workspace_id);
ALTER TABLE semantic_memory ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE semantic_memory ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_semantic_memory_workspace ON semantic_memory(workspace_id);
ALTER TABLE prospect_discovery_run ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE prospect_discovery_run ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_prospect_discovery_run_workspace ON prospect_discovery_run(workspace_id);
ALTER TABLE creative_daily_quota ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE creative_daily_quota ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_creative_daily_quota_workspace ON creative_daily_quota(workspace_id);
ALTER TABLE creative_asset ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE creative_asset ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_creative_asset_workspace ON creative_asset(workspace_id);
ALTER TABLE marketing_subscriber ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_subscriber ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_subscriber_workspace ON marketing_subscriber(workspace_id);
ALTER TABLE marketing_email_campaign ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_email_campaign ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_email_campaign_workspace ON marketing_email_campaign(workspace_id);
ALTER TABLE marketing_email_delivery ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_email_delivery ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_email_delivery_workspace ON marketing_email_delivery(workspace_id);
ALTER TABLE marketing_email_quota ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_email_quota ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_email_quota_workspace ON marketing_email_quota(workspace_id);
ALTER TABLE marketing_observation ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_observation ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_observation_workspace ON marketing_observation(workspace_id);
ALTER TABLE marketing_strategy_weight ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_strategy_weight ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_strategy_weight_workspace ON marketing_strategy_weight(workspace_id);
ALTER TABLE marketing_daily_plan ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE marketing_daily_plan ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_marketing_daily_plan_workspace ON marketing_daily_plan(workspace_id);
ALTER TABLE seo_snapshot ADD COLUMN workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001' REFERENCES workspace(id);
ALTER TABLE seo_snapshot ALTER COLUMN workspace_id SET DEFAULT nullif(current_setting('sarah.workspace_id', true), '')::uuid;
CREATE INDEX idx_seo_snapshot_workspace ON seo_snapshot(workspace_id);

-- A name/registry identifier can recur in another company, but not in the same one.
ALTER TABLE campaign DROP CONSTRAINT IF EXISTS campaign_name_key;
CREATE UNIQUE INDEX ux_campaign_workspace_name ON campaign(workspace_id, lower(name));
ALTER TABLE connector_account DROP CONSTRAINT IF EXISTS connector_account_type_name_key;
ALTER TABLE connector_account ADD CONSTRAINT ux_connector_workspace_type_name UNIQUE(workspace_id, type, name);
ALTER TABLE prospect DROP CONSTRAINT IF EXISTS prospect_registry_siren_key;
CREATE UNIQUE INDEX ux_prospect_workspace_siren ON prospect(workspace_id, registry_siren) WHERE registry_siren IS NOT NULL;

-- Natural keys must be per workspace (not global).
ALTER TABLE prospect_discovery_run DROP CONSTRAINT prospect_discovery_run_pkey;
ALTER TABLE prospect_discovery_run ADD PRIMARY KEY(workspace_id, run_day);
ALTER TABLE creative_daily_quota DROP CONSTRAINT creative_daily_quota_pkey;
ALTER TABLE creative_daily_quota ADD PRIMARY KEY(workspace_id, day);
ALTER TABLE marketing_email_quota DROP CONSTRAINT marketing_email_quota_pkey;
ALTER TABLE marketing_email_quota ADD PRIMARY KEY(workspace_id, day);
ALTER TABLE marketing_strategy_weight DROP CONSTRAINT marketing_strategy_weight_pkey;
ALTER TABLE marketing_strategy_weight ADD PRIMARY KEY(workspace_id, platform);
ALTER TABLE marketing_daily_plan DROP CONSTRAINT marketing_daily_plan_pkey;
ALTER TABLE marketing_daily_plan ADD PRIMARY KEY(workspace_id, day);
ALTER TABLE marketing_email_delivery DROP CONSTRAINT marketing_email_delivery_email_fkey;
ALTER TABLE marketing_subscriber DROP CONSTRAINT marketing_subscriber_pkey;
ALTER TABLE marketing_subscriber ADD PRIMARY KEY(workspace_id, email);
ALTER TABLE marketing_email_delivery ADD CONSTRAINT fk_email_delivery_workspace_subscriber
  FOREIGN KEY(workspace_id, email) REFERENCES marketing_subscriber(workspace_id, email);

-- Composite relationships reject attaching another workspace's resource even with a guessed UUID.
ALTER TABLE campaign ADD CONSTRAINT uq_campaign_workspace_id UNIQUE(workspace_id, id);
ALTER TABLE content_draft ADD CONSTRAINT uq_draft_workspace_id UNIQUE(workspace_id, id);
ALTER TABLE connector_account ADD CONSTRAINT uq_connector_workspace_id UNIQUE(workspace_id, id);
ALTER TABLE publication ADD CONSTRAINT uq_publication_workspace_id UNIQUE(workspace_id, id);
ALTER TABLE marketing_email_campaign ADD CONSTRAINT uq_email_campaign_workspace_id UNIQUE(workspace_id, id);
ALTER TABLE workflow_task ADD CONSTRAINT fk_workflow_workspace_campaign FOREIGN KEY(workspace_id, campaign_id) REFERENCES campaign(workspace_id,id);
ALTER TABLE content_draft ADD CONSTRAINT fk_draft_workspace_campaign FOREIGN KEY(workspace_id, campaign_id) REFERENCES campaign(workspace_id,id);
ALTER TABLE publication ADD CONSTRAINT fk_publication_workspace_campaign FOREIGN KEY(workspace_id, campaign_id) REFERENCES campaign(workspace_id,id);
ALTER TABLE publication ADD CONSTRAINT fk_publication_workspace_draft FOREIGN KEY(workspace_id, draft_id) REFERENCES content_draft(workspace_id,id);
ALTER TABLE publication ADD CONSTRAINT fk_publication_workspace_connector FOREIGN KEY(workspace_id, connector_account_id) REFERENCES connector_account(workspace_id,id);
ALTER TABLE marketing_observation ADD CONSTRAINT fk_observation_workspace_publication FOREIGN KEY(workspace_id, publication_id) REFERENCES publication(workspace_id,id);
ALTER TABLE marketing_daily_plan ADD CONSTRAINT fk_daily_plan_workspace_campaign FOREIGN KEY(workspace_id, campaign_id) REFERENCES campaign(workspace_id,id);
ALTER TABLE marketing_email_delivery ADD CONSTRAINT fk_email_delivery_workspace_campaign FOREIGN KEY(workspace_id, campaign_id) REFERENCES marketing_email_campaign(workspace_id,id);

-- The database role enforced for each /api/v1 request has no BYPASSRLS privilege.
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='sarah_workspace_runtime') THEN
    CREATE ROLE sarah_workspace_runtime NOLOGIN NOINHERIT;
  END IF;
END $$;
GRANT USAGE ON SCHEMA public TO sarah_workspace_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO sarah_workspace_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sarah_workspace_runtime;

-- FORCE applies RLS even if the table owner ever uses this role. No workspace id = no rows.
DO $$
DECLARE tbl TEXT;
BEGIN
  FOREACH tbl IN ARRAY ARRAY['brand_profile','autonomy_settings','campaign','workflow_task','content_draft','connector_account','publication','audit_log','prospect','chat_message','semantic_memory','prospect_discovery_run','creative_daily_quota','creative_asset','marketing_subscriber','marketing_email_campaign','marketing_email_delivery','marketing_email_quota','marketing_observation','marketing_strategy_weight','marketing_daily_plan','seo_snapshot'] LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', tbl);
    EXECUTE format('CREATE POLICY tenant_isolation ON %I USING (workspace_id = nullif(current_setting(''sarah.workspace_id'', true), '''')::uuid) WITH CHECK (workspace_id = nullif(current_setting(''sarah.workspace_id'', true), '''')::uuid)', tbl);
  END LOOP;
END $$;
