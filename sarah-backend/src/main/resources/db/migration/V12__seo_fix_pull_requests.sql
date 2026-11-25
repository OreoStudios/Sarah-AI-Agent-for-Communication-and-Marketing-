-- Workspace-bound GitHub credentials and audit-triggered, user-reviewed remediation PRs.
CREATE TABLE seo_github_connection (
  workspace_id UUID PRIMARY KEY REFERENCES workspace(id) ON DELETE CASCADE,
  repo_owner VARCHAR(100) NOT NULL,
  repo_name VARCHAR(100) NOT NULL,
  encrypted_token TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE seo_fix_pr (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
  snapshot_id BIGINT NOT NULL,
  repo_owner VARCHAR(100) NOT NULL,
  repo_name VARCHAR(100) NOT NULL,
  pr_url VARCHAR(1000) NOT NULL,
  model VARCHAR(120) NOT NULL,
  changes INT NOT NULL CHECK (changes > 0 AND changes <= 8),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, snapshot_id, repo_owner, repo_name)
);
ALTER TABLE seo_snapshot ADD CONSTRAINT uq_seo_snapshot_workspace_id UNIQUE (workspace_id, id);
ALTER TABLE seo_fix_pr ADD CONSTRAINT fk_seo_fix_pr_workspace_snapshot
  FOREIGN KEY (workspace_id, snapshot_id) REFERENCES seo_snapshot (workspace_id, id);
ALTER TABLE seo_github_connection ENABLE ROW LEVEL SECURITY;
ALTER TABLE seo_github_connection FORCE ROW LEVEL SECURITY;
CREATE POLICY seo_github_connection_tenant ON seo_github_connection
  USING (workspace_id = nullif(current_setting('sarah.workspace_id', true), '')::uuid)
  WITH CHECK (workspace_id = nullif(current_setting('sarah.workspace_id', true), '')::uuid);
ALTER TABLE seo_fix_pr ENABLE ROW LEVEL SECURITY;
ALTER TABLE seo_fix_pr FORCE ROW LEVEL SECURITY;
CREATE POLICY seo_fix_pr_tenant ON seo_fix_pr
  USING (workspace_id = nullif(current_setting('sarah.workspace_id', true), '')::uuid)
  WITH CHECK (workspace_id = nullif(current_setting('sarah.workspace_id', true), '')::uuid);
GRANT SELECT, INSERT, UPDATE, DELETE ON seo_github_connection, seo_fix_pr TO sarah_workspace_runtime;
