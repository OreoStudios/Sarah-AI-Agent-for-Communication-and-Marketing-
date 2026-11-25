CREATE TABLE seo_snapshot (
    id BIGSERIAL PRIMARY KEY,
    kind VARCHAR(30) NOT NULL,
    subject VARCHAR(1200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    report JSONB NOT NULL
);
CREATE INDEX seo_snapshot_created_idx ON seo_snapshot(created_at DESC);
