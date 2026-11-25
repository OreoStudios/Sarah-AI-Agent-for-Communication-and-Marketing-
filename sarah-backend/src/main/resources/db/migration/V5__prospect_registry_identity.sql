ALTER TABLE prospect ADD COLUMN registry_siren VARCHAR(9) UNIQUE;
CREATE TABLE prospect_discovery_run (run_day DATE PRIMARY KEY, status VARCHAR(32) NOT NULL, imported INT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
