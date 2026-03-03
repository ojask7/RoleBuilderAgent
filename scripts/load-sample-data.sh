#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Seed the database with sample IAM data for development and demo.
# Creates: security groups, application services, business applications,
#          entitlements, IT roles, business roles, access bundles, and
#          user-SG assignments.
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Load .env if available
if [ -f "${ROOT}/.env" ]; then
  # shellcheck disable=SC1091
  source "${ROOT}/.env"
fi

DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-agentdb}"
DB_USER="${POSTGRES_USER:-agent}"
export PGPASSWORD="${POSTGRES_PASSWORD:-agentpass}"

PSQL="psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -v ON_ERROR_STOP=1"

echo "[seed] Loading sample data into ${DB_NAME} on ${DB_HOST}:${DB_PORT}..."

${PSQL} <<'SQL'
-- ==========================================================================
-- SAMPLE IAM DATA — Finance, IT Ops, HR departments
-- ==========================================================================

BEGIN;

-- Legacy tables (V1 schema)
INSERT INTO security_group (name, description, owner) VALUES
  ('CH_SG_SAP_FI_STG_Read',   'SAP FI staging read access',   'alice@corp.com'),
  ('CH_SG_SAP_FI_PRD_Read',   'SAP FI production read access','alice@corp.com'),
  ('SG_SAP_RFC_FI_ReadOnly',  'SAP RFC FI read-only',         'alice@corp.com'),
  ('SG_PowerBI_Finance_View', 'PowerBI Finance dashboards',   'bob@corp.com'),
  ('SG_Legacy_Reports',       'Legacy reporting system',       NULL),
  ('SG_ITSM_SelfService',     'ITSM self-service portal',     'svcdesk@corp.com'),
  ('SG_HR_Workday_Read',      'Workday HR read access',       'hr-admin@corp.com'),
  ('SG_HR_Workday_Write',     'Workday HR write access',      'hr-admin@corp.com'),
  ('SG_HR_ADP_Payroll',       'ADP payroll system',           'payroll@corp.com'),
  ('SG_Unknown_Legacy_42',    NULL,                            NULL),
  ('SG_Unknown_Legacy_88',    NULL,                            NULL),
  ('SG_AD_DL_Finance_All',    'Finance distribution list',     NULL),
  ('SG_Citrix_Finance_App',   'Citrix published app - Finance',NULL),
  ('SG_VPN_CorpNetwork',      'Corporate VPN access',         'netops@corp.com'),
  ('SG_MFA_Enrolled',         'MFA enrolled users',           'secops@corp.com')
ON CONFLICT DO NOTHING;

INSERT INTO application_service (code, name, owner) VALUES
  ('SAP-FI-STG',       'SAP FI Staging',        'alice@corp.com'),
  ('SAP-FI-PRD',       'SAP FI Production',     'alice@corp.com'),
  ('PowerBI-Finance',  'PowerBI Finance',       'bob@corp.com'),
  ('LegacyReports',    'Legacy Reporting',      'john@corp.com'),
  ('ITSM-Portal',      'ITSM Self-Service',     'svcdesk@corp.com'),
  ('Workday-HR',       'Workday HR',            'hr-admin@corp.com'),
  ('ADP-Payroll',      'ADP Payroll',           'payroll@corp.com')
ON CONFLICT DO NOTHING;

INSERT INTO business_application (code, name, compliance_tier) VALUES
  ('SAP-Finance',  'SAP Finance Platform', 'Tier-1'),
  ('Reporting',    'Reporting Platform',    'Tier-2'),
  ('ITSM',         'IT Service Management','Tier-2'),
  ('HR-Suite',     'HR Suite',             'Tier-1'),
  ('Payroll',      'Payroll System',       'Tier-1')
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------
-- Entitlements (new V3 schema) — the enriched SG catalog
-- --------------------------------------------------------------------------
INSERT INTO entitlement (source_sg_name, source_system, application_service, business_app, description, owner, owner_source, discovered_from, status, confidence) VALUES
  ('CH_SG_SAP_FI_STG_Read',   'AD', 'SAP-FI-STG',      'SAP-Finance', 'SAP FI staging read access',       'alice@corp.com', 'SailPoint', 'AD+SailPoint', 'GOVERNED', 0.98),
  ('CH_SG_SAP_FI_PRD_Read',   'AD', 'SAP-FI-PRD',      'SAP-Finance', 'SAP FI production read access',    'alice@corp.com', 'SailPoint', 'AD+SailPoint', 'GOVERNED', 0.98),
  ('SG_SAP_RFC_FI_ReadOnly',  'AD', 'SAP-FI-PRD',      'SAP-Finance', 'SAP RFC FI read-only integration', 'alice@corp.com', 'CMDB',      'AD+CMDB',      'MAPPED',   0.85),
  ('SG_PowerBI_Finance_View', 'AD', 'PowerBI-Finance',  'Reporting',   'PowerBI Finance dashboards',       'bob@corp.com',   'SailPoint', 'AD+SailPoint', 'GOVERNED', 0.95),
  ('SG_Legacy_Reports',       'AD', 'LegacyReports',    'Reporting',   'Legacy reporting system access',   'john@corp.com',  'MDT',       'AD+CMDB',      'DISCOVERED', 0.72),
  ('SG_ITSM_SelfService',     'AD', 'ITSM-Portal',      'ITSM',        'ITSM self-service portal',         'svcdesk@corp.com','SailPoint','AD+SailPoint', 'GOVERNED', 0.99),
  ('SG_HR_Workday_Read',      'AD', 'Workday-HR',       'HR-Suite',    'Workday HR read access',           'hr-admin@corp.com','SailPoint','AD+SailPoint','GOVERNED', 0.97),
  ('SG_HR_Workday_Write',     'AD', 'Workday-HR',       'HR-Suite',    'Workday HR write access',          'hr-admin@corp.com','SailPoint','AD+SailPoint','GOVERNED', 0.97),
  ('SG_HR_ADP_Payroll',       'AD', 'ADP-Payroll',      'Payroll',     'ADP payroll system access',        'payroll@corp.com','SailPoint', 'AD+SailPoint','GOVERNED', 0.96),
  ('SG_Unknown_Legacy_42',    'AD', NULL,               NULL,          NULL,                                NULL,             NULL,        'AD',           'ORPHAN',   0.0),
  ('SG_Unknown_Legacy_88',    'AD', NULL,               NULL,          NULL,                                NULL,             NULL,        'AD',           'ORPHAN',   0.0),
  ('SG_AD_DL_Finance_All',    'AD', NULL,               NULL,          'Finance distribution list',         NULL,             NULL,        'AD',           'ORPHAN',   0.10),
  ('SG_Citrix_Finance_App',   'AD', NULL,               'SAP-Finance', 'Citrix published app for Finance', NULL,             NULL,        'AD',           'DISCOVERED', 0.55),
  ('SG_VPN_CorpNetwork',      'AD', NULL,               NULL,          'Corporate VPN access',             'netops@corp.com','AD',        'AD',           'MAPPED',   0.60),
  ('SG_MFA_Enrolled',         'AD', NULL,               NULL,          'MFA enrolled users',               'secops@corp.com','AD',        'AD',           'MAPPED',   0.60)
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------
-- IT Roles — bundles of entitlements per application
-- --------------------------------------------------------------------------
INSERT INTO it_role (name, description, application_id, owner, status, mining_source, confidence) VALUES
  ('SAP-FI-Reader',       'Read access to SAP FI across staging and production',  'SAP-Finance', 'alice@corp.com',   'ACTIVE',    'role-mining-v1', 0.94),
  ('Reporting-Viewer',    'View access to all reporting tools',                   'Reporting',   'bob@corp.com',     'ACTIVE',    'role-mining-v1', 0.87),
  ('ServiceDesk-Basic',   'Basic ITSM self-service access',                       'ITSM',        'svcdesk@corp.com', 'ACTIVE',    'role-mining-v1', 0.92),
  ('HR-Read',             'Read access to HR systems',                            'HR-Suite',    'hr-admin@corp.com','APPROVED',  'role-mining-v1', 0.90),
  ('HR-Admin',            'Full HR administration access',                        'HR-Suite',    'hr-admin@corp.com','APPROVED',  'role-mining-v1', 0.88),
  ('Payroll-Processor',   'Payroll processing access',                            'Payroll',     'payroll@corp.com', 'SUGGESTED', 'role-mining-v1', 0.75)
ON CONFLICT DO NOTHING;

-- IT Role <-> Entitlement mappings
INSERT INTO it_role_entitlement (it_role_id, entitlement_id)
SELECT ir.id, e.id FROM it_role ir, entitlement e
WHERE (ir.name = 'SAP-FI-Reader'    AND e.source_sg_name IN ('CH_SG_SAP_FI_STG_Read', 'CH_SG_SAP_FI_PRD_Read', 'SG_SAP_RFC_FI_ReadOnly'))
   OR (ir.name = 'Reporting-Viewer'  AND e.source_sg_name IN ('SG_PowerBI_Finance_View', 'SG_Legacy_Reports'))
   OR (ir.name = 'ServiceDesk-Basic' AND e.source_sg_name IN ('SG_ITSM_SelfService'))
   OR (ir.name = 'HR-Read'           AND e.source_sg_name IN ('SG_HR_Workday_Read'))
   OR (ir.name = 'HR-Admin'          AND e.source_sg_name IN ('SG_HR_Workday_Read', 'SG_HR_Workday_Write'))
   OR (ir.name = 'Payroll-Processor' AND e.source_sg_name IN ('SG_HR_ADP_Payroll'))
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------
-- Business Roles — bundles of IT Roles per job function
-- --------------------------------------------------------------------------
INSERT INTO business_role (name, job_function, department, region, owner, description, status, confidence) VALUES
  ('Finance-Analyst-EMEA', 'Finance Analyst', 'Finance', 'EMEA', 'alice@corp.com',
   'Standard access bundle for EMEA Finance Analysts — includes SAP FI read, reporting, and basic service desk.',
   'ACTIVE', 0.91),
  ('HR-Specialist-Global',  'HR Specialist',  'HR',      NULL,   'hr-admin@corp.com',
   'HR specialist access — Workday read + ITSM self-service.',
   'DRAFT', 0.85),
  ('HR-Admin-Global',       'HR Administrator','HR',     NULL,   'hr-admin@corp.com',
   'HR admin access — full Workday + payroll + ITSM.',
   'DRAFT', 0.82)
ON CONFLICT DO NOTHING;

-- Business Role <-> IT Role mappings
INSERT INTO business_role_it_role (business_role_id, it_role_id)
SELECT br.id, ir.id FROM business_role br, it_role ir
WHERE (br.name = 'Finance-Analyst-EMEA' AND ir.name IN ('SAP-FI-Reader', 'Reporting-Viewer', 'ServiceDesk-Basic'))
   OR (br.name = 'HR-Specialist-Global' AND ir.name IN ('HR-Read', 'ServiceDesk-Basic'))
   OR (br.name = 'HR-Admin-Global'      AND ir.name IN ('HR-Admin', 'Payroll-Processor', 'ServiceDesk-Basic'))
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------
-- Access Bundles — lifecycle-managed wrappers
-- --------------------------------------------------------------------------
INSERT INTO access_bundle (business_role_id, version, status, approved_by, approved_at, kc27_status, total_entitlements)
SELECT br.id, 1, 'ACTIVE', 'iam-governance@corp.com', NOW() - INTERVAL '30 days', 'PARTIALLY_COMPLIANT', 6
FROM business_role br WHERE br.name = 'Finance-Analyst-EMEA'
ON CONFLICT DO NOTHING;

INSERT INTO access_bundle (business_role_id, version, status, kc27_status, total_entitlements)
SELECT br.id, 1, 'DRAFT', 'NOT_ASSESSED', 2
FROM business_role br WHERE br.name = 'HR-Specialist-Global'
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------
-- User <-> Business Role assignments (sample population)
-- --------------------------------------------------------------------------
INSERT INTO user_business_role (user_id, business_role_id, assigned_by)
SELECT u.uid, br.id, 'seed-script'
FROM (VALUES ('U001'), ('U002'), ('U003'), ('U004'), ('U005'),
             ('U006'), ('U007'), ('U008'), ('U009'), ('U010'),
             ('U011'), ('U012')) AS u(uid),
     business_role br
WHERE br.name = 'Finance-Analyst-EMEA'
ON CONFLICT DO NOTHING;

INSERT INTO user_business_role (user_id, business_role_id, assigned_by)
SELECT u.uid, br.id, 'seed-script'
FROM (VALUES ('U020'), ('U021'), ('U022')) AS u(uid),
     business_role br
WHERE br.name = 'HR-Specialist-Global'
ON CONFLICT DO NOTHING;

COMMIT;

-- Summary
SELECT 'security_group'    AS entity, COUNT(*) AS count FROM security_group
UNION ALL SELECT 'application_service', COUNT(*) FROM application_service
UNION ALL SELECT 'business_application', COUNT(*) FROM business_application
UNION ALL SELECT 'entitlement',          COUNT(*) FROM entitlement
UNION ALL SELECT 'it_role',              COUNT(*) FROM it_role
UNION ALL SELECT 'business_role',        COUNT(*) FROM business_role
UNION ALL SELECT 'access_bundle',        COUNT(*) FROM access_bundle
UNION ALL SELECT 'user_business_role',   COUNT(*) FROM user_business_role
UNION ALL SELECT 'it_role_entitlement',  COUNT(*) FROM it_role_entitlement
UNION ALL SELECT 'business_role_it_role',COUNT(*) FROM business_role_it_role
ORDER BY entity;
SQL

echo "[seed] Done. Sample data loaded successfully."
