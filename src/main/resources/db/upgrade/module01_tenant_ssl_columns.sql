-- =====================================================================================================
-- authservice upgrade: SSL flags on tenant_isolation_config (TenantIsolationConfig.sslEnabled and
-- sslAutoRenewEnabled, both true by default). Without these columns Hibernate cannot add them as NOT NULL
-- to a table that already has rows, and authservice fails to start.
-- Idempotent; run once per database before starting the new authservice build:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
-- =====================================================================================================
BEGIN;

ALTER TABLE tenant_isolation_config
    ADD COLUMN IF NOT EXISTS ssl_enabled boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS ssl_auto_renew_enabled boolean NOT NULL DEFAULT true;

COMMIT;
