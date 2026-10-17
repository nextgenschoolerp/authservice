-- =====================================================================================================
-- Seed data: Module 01 — Authentication, Authorization, Security & Multi-Tenancy (authservice)
-- Target DB : jdbc:postgresql://localhost:5432/authservice (tables created by Hibernate from the entities)
--
-- Builds on the existing base rows (tenants DPS a0eEBc99-… and SVKM b1eEBc99-…, roles 1-10,
-- users admin_rajesh / principal_anita / teacher_vikram) and adds:
--   * a third tenant, Greenfield Montessori Education Trust, with full isolation / MFA / session config
--   * the Module 02 admission roles and the staff and parent accounts referenced by the module 02 seed
--     (meera.counselor@dps.edu.in, deepak.admissions@dps.edu.in, rohit.sharma@gmail.com, …)
--   * one account per Module 01 role, plus edge cases: an inactive ex-teacher with revoked sessions,
--     a locked-out teacher, and a principal who must change the password on first login
--   * sessions, API tokens, reset tokens, delegations, anonymization logs, encryption policies,
--     alert rules, backups and audit entries for those accounts
--
-- Login password for every seeded account: Password@123 (BCrypt, cost 10)
--
-- Re-runnable: every row is marked created_by = 'seed:module01' and seeded users have user_id 5eed0001-…;
-- the script deletes only those rows first. Existing base rows are never modified.
-- Validity windows (sessions, tokens, delegations) are relative to now(), so they stay current.
-- =====================================================================================================

BEGIN;

-- ---------------------------------------------------------------------------------------------------
-- 0. Clean previous seed rows (children first) and realign id sequences with existing data
-- ---------------------------------------------------------------------------------------------------
DELETE FROM audit_logs                  WHERE created_by = 'seed:module01';
DELETE FROM role_permissions            WHERE created_by = 'seed:module01';
DELETE FROM role_delegations            WHERE created_by = 'seed:module01';
DELETE FROM user_sessions               WHERE created_by = 'seed:module01';
DELETE FROM user_roles                  WHERE created_by = 'seed:module01';
DELETE FROM user_credentials            WHERE created_by = 'seed:module01';
DELETE FROM password_reset_tokens       WHERE created_by = 'seed:module01';
DELETE FROM api_tokens                  WHERE created_by = 'seed:module01';
DELETE FROM privacy_anonymization_logs  WHERE created_by = 'seed:module01';
DELETE FROM field_encryption_policies   WHERE created_by = 'seed:module01';
DELETE FROM security_alert_rules        WHERE created_by = 'seed:module01';
DELETE FROM disaster_recovery_backups   WHERE created_by = 'seed:module01';
DELETE FROM users                       WHERE created_by = 'seed:module01';
DELETE FROM roles                       WHERE created_by = 'seed:module01';
DELETE FROM branches_provisioning       WHERE created_by = 'seed:module01';
DELETE FROM mfa_config                  WHERE created_by = 'seed:module01';
DELETE FROM session_config              WHERE created_by = 'seed:module01';
DELETE FROM tenant_isolation_config     WHERE created_by = 'seed:module01';
DELETE FROM tenants                     WHERE created_by = 'seed:module01';

-- Base rows were inserted with explicit ids; make sure every sequence is past the highest id
DO $$
DECLARE t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['tenants','branches_provisioning','tenant_isolation_config','mfa_config','session_config','roles',
                             'role_permissions','users','user_roles','user_credentials','user_sessions','api_tokens',
                             'password_reset_tokens','role_delegations','privacy_anonymization_logs','field_encryption_policies',
                             'security_alert_rules','disaster_recovery_backups','audit_logs']
    LOOP
        EXECUTE format('SELECT setval(pg_get_serial_sequence(%L, ''id''), GREATEST((SELECT COALESCE(MAX(id), 0) FROM %I), '
                       '(SELECT last_value FROM %s)), true)', t, t, pg_get_serial_sequence(t, 'id'));
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------------------------------
-- 1. US-00000 / US-00009 Tenant onboarding and branch provisioning
-- ---------------------------------------------------------------------------------------------------
INSERT INTO tenants (tenant_id, society_code, society_name, domain_name, contact_email, contact_phone, status,
                     created_at, updated_at, created_by, updated_by, version)
VALUES ('5eed0001-0000-4000-8000-000000000003', 'GMET-SOC-03', 'Greenfield Montessori Education Trust', 'greenfield.edu.in',
        'admin@greenfield.edu.in', '+918041234567', 'ACTIVE', '2026-06-02 05:30:00+00', '2026-06-02 05:30:00+00', 'seed:module01', NULL, 0);

INSERT INTO branches_provisioning (tenant_id, branch_code, branch_name, affiliation_board, schema_name, student_capacity, staff_capacity,
                                   isolated_db_enabled, status, created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0001-0000-4000-8000-000000000003', 'GMET-BLR-WHITEFIELD', 'Greenfield Montessori Whitefield Campus', 'IB',   'tenant_gmet_whitefield', 1200, 110, true,  'PROVISIONED', '2026-06-02 06:00:00+00', '2026-06-02 06:00:00+00', 'seed:module01', NULL, 0),
('5eed0001-0000-4000-8000-000000000003', 'GMET-BLR-JAYANAGAR',  'Greenfield Montessori Jayanagar Campus',  'CBSE', 'tenant_gmet_jayanagar',   900,  85, false, 'PROVISIONED', '2026-06-20 06:00:00+00', '2026-06-20 06:00:00+00', 'seed:module01', NULL, 0),
('b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'SVKM-PUNE-KOTHRUD',   'SVKM Kothrud English Medium School',      'ICSE', 'tenant_svkm_pune',       1800, 150, true,  'PROVISIONED', '2026-07-15 04:30:00+00', '2026-07-15 04:30:00+00', 'seed:module01', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 2. US-00001 / US-00016 Tenant isolation & SSL, US-00003 MFA, US-00004/12/13/17 Session, IP, lockout, password policy
--    (one row per tenant — only for the new tenant; DPS and SVKM already have theirs)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO tenant_isolation_config (tenant_id, schema_name, row_level_security_enabled, cross_tenant_access_blocked, encryption_algorithm,
                                     domain_name, ssl_expires_at, tls_version, created_at, updated_at, created_by, updated_by, version)
VALUES ('5eed0001-0000-4000-8000-000000000003', 'tenant_gmet_soc_03', true, true, 'AES-256-GCM',
        'erp.greenfield.edu.in', '2027-03-31 23:59:59+00', 'TLSv1.3', '2026-06-02 06:10:00+00', '2026-09-01 04:00:00+00', 'seed:module01', 'lavanya.rao@greenfield.edu.in', 0);

INSERT INTO mfa_config (tenant_id, totp_enabled, sms_enabled, mandatory_for_sensitive_transactions, max_failed_attempts,
                        created_at, updated_at, created_by, updated_by, version)
VALUES ('5eed0001-0000-4000-8000-000000000003', true, true, true, 3, '2026-06-02 06:15:00+00', '2026-06-02 06:15:00+00', 'seed:module01', NULL, 0);

INSERT INTO session_config (tenant_id, concurrent_sessions_allowed, max_concurrent_sessions, idle_timeout_minutes, max_session_duration_minutes,
                            allowed_subnets, enforce_campus_subnet, auto_unlock_enabled, max_failed_attempts, lockout_duration_minutes,
                            min_password_length, require_uppercase, require_lowercase, require_digits, require_special_chars,
                            password_expiry_days, prevent_reuse_count, created_at, updated_at, created_by, updated_by, version)
VALUES ('5eed0001-0000-4000-8000-000000000003', true, 2, 20, 600, '10.30.0.0/16,103.21.58.0/24', true, true, 5, 30,
        10, true, true, true, true, 120, 5, '2026-06-02 06:20:00+00', '2026-08-18 05:00:00+00', 'seed:module01', 'lavanya.rao@greenfield.edu.in', 0);

-- ---------------------------------------------------------------------------------------------------
-- 3. US-00002 Roles and screen permissions
-- ---------------------------------------------------------------------------------------------------
INSERT INTO roles (name, description, active, created_at, updated_at, created_by, updated_by, version)
VALUES
('ADMISSION_OFFICER',       'Schedules entrance exams, ranks candidates and manages the admission pipeline', true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('FRONT_OFFICE_COUNSELOR',  'Captures enquiries and nurtures leads at the admissions desk',                  true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('REGISTRAR',               'Evaluates applicants and verifies admission documents',                         true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('ENTRANCE_EXAM_EVALUATOR', 'Marks entrance tests and uploads applicant records to the document vault',      true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('FINANCE_CASHIER',         'Issues offer letters with payment links and collects admission fees',           true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('MARKETING_MANAGER',       'Tracks recruitment campaigns and lead conversion analytics',                    true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('SCHOOL_PRINCIPAL',        'Approves admissions and collects seat reservation deposits',                    true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0),
('APPLICANT_PARENT',        'Prospective parent with access to their own child''s application',             true, '2026-07-01 04:30:00+00', '2026-07-01 04:30:00+00', 'seed:module01', NULL, 0);

INSERT INTO role_permissions (role_id, screen_name, can_read, can_write, can_delete, can_export, created_at, updated_at, created_by, updated_by, version)
SELECT r.id, p.screen_name, p.can_read, p.can_write, p.can_delete, p.can_export, '2026-07-01 04:45:00+00', '2026-07-01 04:45:00+00', 'seed:module01', NULL, 0
FROM (VALUES
    -- Module 02 admission roles
    ('ADMISSION_OFFICER',       'ADMISSION_ENQUIRY',       true,  true,  false, true),
    ('ADMISSION_OFFICER',       'ENTRANCE_EXAM',           true,  true,  true,  true),
    ('ADMISSION_OFFICER',       'MERIT_LIST',              true,  true,  false, true),
    ('ADMISSION_OFFICER',       'OFFER_LETTER',            true,  true,  false, true),
    ('FRONT_OFFICE_COUNSELOR',  'ADMISSION_ENQUIRY',       true,  true,  false, false),
    ('FRONT_OFFICE_COUNSELOR',  'APPLICANT_COMMUNICATION', true,  true,  false, false),
    ('REGISTRAR',               'APPLICANT_EVALUATION',    true,  true,  false, true),
    ('REGISTRAR',               'DOCUMENT_VAULT',          true,  true,  false, false),
    ('REGISTRAR',               'STUDENT_CONVERSION',      true,  true,  false, true),
    ('ENTRANCE_EXAM_EVALUATOR', 'DOCUMENT_VAULT',          true,  true,  false, false),
    ('ENTRANCE_EXAM_EVALUATOR', 'APPLICANT_EVALUATION',    true,  false, false, false),
    ('FINANCE_CASHIER',         'OFFER_LETTER',            true,  true,  false, true),
    ('FINANCE_CASHIER',         'FEE_DEPOSIT',             true,  true,  false, true),
    ('MARKETING_MANAGER',       'CRM_ANALYTICS',           true,  false, false, true),
    ('SCHOOL_PRINCIPAL',        'FEE_DEPOSIT',             true,  true,  false, true),
    ('SCHOOL_PRINCIPAL',        'MERIT_LIST',              true,  false, false, true),
    ('APPLICANT_PARENT',        'STUDENT_CONVERSION',      true,  true,  false, false),
    -- Module 01 roles that had no screen permissions yet
    ('TRUSTEE',                 'DISASTER_RECOVERY',       true,  true,  false, true),
    ('TRUSTEE',                 'BRANCH_PROVISIONING',     true,  false, false, true),
    ('FINANCE_OFFICER',         'FEE_COLLECTION',          true,  true,  false, true),
    ('IT_SYSTEM_ADMIN',         'SESSION_SECURITY',        true,  true,  false, false),
    ('IT_SYSTEM_ADMIN',         'IP_WHITELIST',            true,  true,  true,  false),
    ('SECURITY_AUDITOR',        'AUDIT_LOGS',              true,  false, false, true),
    ('SECURITY_AUDITOR',        'LOCKOUT_POLICY',          true,  true,  false, false),
    ('DEPARTMENT_HEAD',         'ROLE_DELEGATION',         true,  true,  true,  false),
    ('ADMIN_CLERK',             'API_TOKENS',              true,  true,  true,  false),
    ('COMPLIANCE_OFFICER',      'PRIVACY_COMPLIANCE',      true,  true,  false, true),
    ('COMPLIANCE_OFFICER',      'FIELD_ENCRYPTION',        true,  true,  false, false)
) AS p(role_name, screen_name, can_read, can_write, can_delete, can_export)
JOIN roles r ON r.name = p.role_name;

-- ---------------------------------------------------------------------------------------------------
-- 4. US-00000 Users, credentials (Password@123) and role assignments
-- ---------------------------------------------------------------------------------------------------
CREATE TEMP TABLE seed_users (user_id text, tenant_id text, branch_id text, username text, first_name text, last_name text, email text,
                              phone text, user_type text, active boolean, mfa boolean, created_at timestamptz) ON COMMIT DROP;
INSERT INTO seed_users VALUES
-- DPS: one account per Module 01 role
('5eed0001-0001-4000-8000-000000000001', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'trustee_arvind',     'Arvind',  'Khanna',     'arvind.khanna@dps.edu.in',      '+919811100101', 'TRUSTEE',                 true,  true,  '2026-04-01 04:30:00+00'),
('5eed0001-0001-4000-8000-000000000002', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'itadmin_nitin',      'Nitin',   'Rao',        'nitin.rao@dps.edu.in',          '+919811100102', 'IT_SYSTEM_ADMIN',         true,  true,  '2026-04-01 04:35:00+00'),
('5eed0001-0001-4000-8000-000000000003', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'auditor_pooja',      'Pooja',   'Bansal',     'pooja.bansal@dps.edu.in',       '+919811100103', 'SECURITY_AUDITOR',        true,  false, '2026-04-02 05:00:00+00'),
('5eed0001-0001-4000-8000-000000000004', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'depthead_sameer',    'Sameer',  'Kulkarni',   'sameer.kulkarni@dps.edu.in',    '+919811100104', 'DEPARTMENT_HEAD',         true,  false, '2026-04-02 05:10:00+00'),
('5eed0001-0001-4000-8000-000000000005', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'clerk_rakesh',       'Rakesh',  'Yadav',      'rakesh.yadav@dps.edu.in',       '+919811100105', 'ADMIN_CLERK',             true,  false, '2026-04-03 06:00:00+00'),
('5eed0001-0001-4000-8000-000000000006', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'compliance_shalini', 'Shalini', 'Mehra',      'shalini.mehra@dps.edu.in',      '+919811100106', 'COMPLIANCE_OFFICER',      true,  true,  '2026-04-03 06:10:00+00'),
('5eed0001-0001-4000-8000-000000000007', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'finance_geeta',      'Geeta',   'Arora',      'geeta.arora@dps.edu.in',        '+919811100107', 'FINANCE_OFFICER',         true,  true,  '2026-04-03 06:20:00+00'),
-- DPS: Module 02 admission team (emails referenced by the module 02 seed)
('5eed0001-0001-4000-8000-000000000008', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'admissions_deepak',  'Deepak',  'Chauhan',    'deepak.admissions@dps.edu.in',  '+919811100108', 'ADMISSION_OFFICER',       true,  false, '2026-07-01 05:00:00+00'),
('5eed0001-0001-4000-8000-000000000009', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'counselor_meera',    'Meera',   'Pillai',     'meera.counselor@dps.edu.in',    '+919811100109', 'FRONT_OFFICE_COUNSELOR',  true,  false, '2026-07-01 05:05:00+00'),
('5eed0001-0001-4000-8000-000000000010', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'registrar_suresh',   'Suresh',  'Nambiar',    'suresh.registrar@dps.edu.in',   '+919811100110', 'REGISTRAR',               true,  false, '2026-07-01 05:10:00+00'),
('5eed0001-0001-4000-8000-000000000011', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'evaluator_kavya',    'Kavya',   'Srinivasan', 'kavya.evaluator@dps.edu.in',    '+919811100111', 'ENTRANCE_EXAM_EVALUATOR', true,  false, '2026-07-01 05:15:00+00'),
('5eed0001-0001-4000-8000-000000000012', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'cashier_ramesh',     'Ramesh',  'Tiwari',     'ramesh.accounts@dps.edu.in',    '+919811100112', 'FINANCE_CASHIER',         true,  false, '2026-07-01 05:20:00+00'),
('5eed0001-0001-4000-8000-000000000013', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'marketing_tanya',    'Tanya',   'Sethi',      'tanya.marketing@dps.edu.in',    '+919811100113', 'MARKETING_MANAGER',       true,  false, '2026-07-01 05:25:00+00'),
('5eed0001-0001-4000-8000-000000000014', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'parent_rohit',       'Rohit',   'Sharma',     'rohit.sharma@gmail.com',        '+919810012301', 'APPLICANT_PARENT',        true,  false, '2026-08-04 05:15:00+00'),
-- DPS: edge cases
('5eed0001-0001-4000-8000-000000000015', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'teacher_neeraj',     'Neeraj',  'Bhatia',     'neeraj.bhatia@dps.edu.in',      '+919811100115', 'TEACHER',                 false, false, '2025-06-10 04:30:00+00'),
('5eed0001-0001-4000-8000-000000000016', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', 'teacher_farhan',     'Farhan',  'Qureshi',    'farhan.qureshi@dps.edu.in',     '+919811100116', 'TEACHER',                 true,  false, '2026-04-15 04:30:00+00'),
('5eed0001-0001-4000-8000-000000000017', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-SOUTH', 'principal_sunita',   'Sunita',  'Rawat',      'sunita.rawat@dps.edu.in',       '+919811100117', 'PRINCIPAL',               true,  true,  '2026-04-01 05:00:00+00'),
-- SVKM
('5eed0001-0001-4000-8000-000000000018', 'b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'SVKM-MUMBAI-MAIN', 'superadmin_mahesh', 'Mahesh',  'Iyer',       'mahesh.iyer@svkm.org',          '+919820011118', 'SUPER_ADMIN',             true,  true,  '2026-03-20 05:00:00+00'),
('5eed0001-0001-4000-8000-000000000019', 'b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'SVKM-MUMBAI-MAIN', 'principal_vandana', 'Vandana', 'Joshi',      'vandana.joshi@svkm.org',        '+919820011119', 'PRINCIPAL',               true,  false, '2026-03-21 05:00:00+00'),
('5eed0001-0001-4000-8000-000000000020', 'b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'SVKM-MUMBAI-MAIN', 'counselor_farah',   'Farah',   'Shaikh',     'farah.counselor@svkm.org',      '+919820011120', 'FRONT_OFFICE_COUNSELOR',  true,  false, '2026-07-02 05:00:00+00'),
-- Greenfield
('5eed0001-0001-4000-8000-000000000021', '5eed0001-0000-4000-8000-000000000003', 'GMET-BLR-WHITEFIELD', 'superadmin_lavanya', 'Lavanya', 'Rao',   'lavanya.rao@greenfield.edu.in', '+918041234501', 'SUPER_ADMIN',             true,  true,  '2026-06-02 05:30:00+00'),
('5eed0001-0001-4000-8000-000000000022', '5eed0001-0000-4000-8000-000000000003', 'GMET-BLR-WHITEFIELD', 'principal_george',   'George',  'Mathew', 'george.mathew@greenfield.edu.in','+918041234502', 'PRINCIPAL',               true,  false, '2026-10-15 06:00:00+00');

INSERT INTO users (user_id, tenant_id, branch_id, username, first_name, last_name, email, phone_number, user_type, is_active, is_mfa_enabled,
                   created_at, updated_at, created_by, updated_by, version)
SELECT user_id, tenant_id, branch_id, username, first_name, last_name, email, phone, user_type, active, mfa,
       created_at, CASE WHEN active THEN created_at ELSE TIMESTAMPTZ '2026-09-30 12:00:00+00' END, 'seed:module01',
       CASE WHEN active THEN NULL ELSE 'rajesh.admin@dps.edu.in' END, 0
FROM seed_users;

INSERT INTO user_credentials (user_id, password_hash, password_salt, failed_login_attempts, locked_until, must_change_password,
                              password_changed_at, created_at, updated_at, created_by, updated_by, version)
SELECT u.user_id,
       '$2a$10$8liDxOW98AN4N7eJQbqKd.LMq6EZ7rg6Q11jCO3sxeqrp39CXcXTq',                 -- Password@123
       'salt_' || substr(md5(u.user_id), 1, 8),
       CASE u.username WHEN 'teacher_farhan' THEN 5 WHEN 'marketing_tanya' THEN 2 ELSE 0 END,
       CASE u.username WHEN 'teacher_farhan' THEN now() + interval '25 minutes' END,      -- US-00013 lockout
       u.username = 'principal_george',                                                  -- US-01004 first-login change
       CASE WHEN u.username = 'principal_george' THEN NULL ELSE u.created_at + interval '10 minutes' END,
       u.created_at, u.created_at, 'seed:module01', NULL, 0
FROM seed_users u;

INSERT INTO user_roles (user_id, role_code, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, user_type, created_at, created_at, 'seed:module01', NULL, 0 FROM seed_users
UNION ALL
-- principal_anita (base seed) also collects admission deposits in module 02
SELECT 'u2222222-2222-2222-2222-222222222222', 'SCHOOL_PRINCIPAL', TIMESTAMPTZ '2026-07-01 05:30:00+00', TIMESTAMPTZ '2026-07-01 05:30:00+00', 'seed:module01', NULL, 0;

-- ---------------------------------------------------------------------------------------------------
-- 5. US-00004 / US-00014 Sessions (active staff sessions; ex-teacher's sessions revoked on resignation)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO user_sessions (session_id, user_id, tenant_id, session_token_hash, ip_address, user_agent, device_fingerprint,
                           login_at, last_activity_at, expires_at, is_revoked, created_at, updated_at, created_by, updated_by, version)
SELECT s.session_id, s.user_id, s.tenant_id, encode(sha256(convert_to('session:' || s.session_id, 'UTF8')), 'hex'), s.ip, s.ua, s.fp,
       s.login_at, s.last_at, s.login_at + interval '24 hours', s.revoked, s.login_at, COALESCE(s.revoked_at, s.last_at), 'seed:module01',
       CASE WHEN s.revoked THEN 'sameer.kulkarni@dps.edu.in' END, 0
FROM (VALUES
    ('5eed0001-0005-4000-8000-000000000001', '5eed0001-0001-4000-8000-000000000009', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '10.20.1.15',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Safari/537.36', 'fp-dps-frontdesk-01',
     now() - interval '2 hours', now() - interval '10 minutes', false, NULL::timestamptz),
    ('5eed0001-0005-4000-8000-000000000002', '5eed0001-0001-4000-8000-000000000008', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '10.20.1.22',
     'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15', 'fp-dps-admissions-mbp',
     now() - interval '3 hours', now() - interval '25 minutes', false, NULL),
    ('5eed0001-0005-4000-8000-000000000003', 'u2222222-2222-2222-2222-222222222222', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '10.20.1.10',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edg/129.0 Safari/537.36', 'fp-dps-principal-office',
     now() - interval '1 hour', now() - interval '5 minutes', false, NULL),
    ('5eed0001-0005-4000-8000-000000000004', '5eed0001-0001-4000-8000-000000000014', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '49.36.112.87',
     'KVS-ParentApp/3.4.1 (Android 14; Samsung SM-A546E)', 'fp-android-7f3a91c2',
     now() - interval '40 minutes', now() - interval '35 minutes', false, NULL),
    ('5eed0001-0005-4000-8000-000000000005', '5eed0001-0001-4000-8000-000000000021', '5eed0001-0000-4000-8000-000000000003', '10.30.4.21',
     'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0', 'fp-gmet-admin-thinkpad',
     now() - interval '4 hours', now() - interval '1 hour', false, NULL),
    ('5eed0001-0005-4000-8000-000000000006', '5eed0001-0001-4000-8000-000000000015', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '10.20.2.41',
     'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36', 'fp-dps-staffroom-07',
     TIMESTAMPTZ '2026-09-30 03:15:00+00', TIMESTAMPTZ '2026-09-30 09:40:00+00', true, TIMESTAMPTZ '2026-09-30 12:00:00+00'),
    ('5eed0001-0005-4000-8000-000000000007', '5eed0001-0001-4000-8000-000000000015', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '106.215.88.14',
     'KVS-StaffApp/2.9.0 (iOS 17.6; iPhone 13)', 'fp-ios-0c55e218',
     TIMESTAMPTZ '2026-09-30 10:05:00+00', TIMESTAMPTZ '2026-09-30 11:50:00+00', true, TIMESTAMPTZ '2026-09-30 12:00:00+00')
) AS s(session_id, user_id, tenant_id, ip, ua, fp, login_at, last_at, revoked, revoked_at);

-- ---------------------------------------------------------------------------------------------------
-- 6. US-00007 API tokens (token_hash = SHA-256 of the raw token, as ApiTokenService stores it)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO api_tokens (tenant_id, user_id, token_name, token_hash, scopes, rate_limit_rpm, expires_at, is_active,
                        created_at, updated_at, created_by, updated_by, version)
VALUES
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0001-0001-4000-8000-000000000005', 'Tally ERP Fee Ledger Sync',
 encode(sha256(convert_to('kvs_live_tally_dps_8f2c1e9a4b7d', 'UTF8')), 'hex'), 'read:fees,write:fee_ledger', 600,
 now() + interval '60 days', true, now() - interval '30 days', now() - interval '30 days', 'seed:module01', NULL, 0),
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0001-0001-4000-8000-000000000005', 'Library RFID Gate Reader',
 encode(sha256(convert_to('kvs_live_rfid_dps_3a6e0d2f9c18', 'UTF8')), 'hex'), 'read:students,write:library_issues', 60,
 now() - interval '5 days', false, now() - interval '95 days', now() - interval '5 days', 'seed:module01', 'rakesh.yadav@dps.edu.in', 0),
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0001-0001-4000-8000-000000000008', 'Admissions Website Lead Form',
 encode(sha256(convert_to('kvs_live_webform_dps_c47b19e2a05f', 'UTF8')), 'hex'), 'write:admission_enquiries', 300,
 now() + interval '25 days', true, now() - interval '5 days', now() - interval '5 days', 'seed:module01', NULL, 0),
('b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', '5eed0001-0001-4000-8000-000000000018', 'Transport GPS Tracker',
 encode(sha256(convert_to('kvs_live_gps_svkm_71d0b8e35c2a', 'UTF8')), 'hex'), 'read:transport_routes,write:vehicle_locations', 1200,
 now() + interval '20 days', true, now() - interval '10 days', now() - interval '10 days', 'seed:module01', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 7. US-01001 / US-01002 Password reset tokens
-- ---------------------------------------------------------------------------------------------------
INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, is_used, created_at, updated_at, created_by, updated_by, version)
VALUES
-- locked-out teacher requested a reset a few minutes ago (still valid for ~12 minutes)
('5eed0001-0001-4000-8000-000000000016', encode(sha256(convert_to('reset:farhan:active', 'UTF8')), 'hex'), now() + interval '12 minutes', false,
 now() - interval '3 minutes', now() - interval '3 minutes', 'seed:module01', NULL, 0),
-- used last week by the marketing manager
('5eed0001-0001-4000-8000-000000000013', encode(sha256(convert_to('reset:tanya:used', 'UTF8')), 'hex'), TIMESTAMPTZ '2026-10-09 06:15:00+00', true,
 TIMESTAMPTZ '2026-10-09 06:00:00+00', TIMESTAMPTZ '2026-10-09 06:04:00+00', 'seed:module01', NULL, 0),
-- requested by the parent but never used; expired
('5eed0001-0001-4000-8000-000000000014', encode(sha256(convert_to('reset:rohit:expired', 'UTF8')), 'hex'), TIMESTAMPTZ '2026-09-12 14:45:00+00', false,
 TIMESTAMPTZ '2026-09-12 14:30:00+00', TIMESTAMPTZ '2026-09-12 14:30:00+00', 'seed:module01', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 8. US-00006 Role delegations
-- ---------------------------------------------------------------------------------------------------
INSERT INTO role_delegations (tenant_id, delegator_user_id, delegatee_user_id, role_id, role_name, start_time, end_time, status, reason,
                              created_at, updated_at, created_by, updated_by, version)
SELECT d.tenant_id, d.delegator, d.delegatee, r.id, d.role_name, d.start_time, d.end_time, d.status, d.reason,
       d.created_at, d.updated_at, 'seed:module01', d.updated_by, 0
FROM (VALUES
    ('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0001-0001-4000-8000-000000000004', 'u3333333-3333-3333-3333-333333333333', 'DEPARTMENT_HEAD',
     now() - interval '1 day', now() + interval '6 days', 'ACTIVE',
     'Covering Science department duties while Dr. Kulkarni attends CBSE evaluator training in Chandigarh.',
     now() - interval '2 days', now() - interval '2 days', NULL),
    ('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0001-0001-4000-8000-000000000008', '5eed0001-0001-4000-8000-000000000009', 'ADMISSION_OFFICER',
     now() + interval '3 days', now() + interval '10 days', 'ACTIVE',
     'Admissions officer on leave during Diwali week; counselor approves exam schedules in the interim.',
     now() - interval '1 day', now() - interval '1 day', NULL),
    ('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'u2222222-2222-2222-2222-222222222222', '5eed0001-0001-4000-8000-000000000017', 'PRINCIPAL',
     TIMESTAMPTZ '2026-08-10 02:30:00+00', TIMESTAMPTZ '2026-08-24 12:30:00+00', 'REVOKED',
     'Annual leave cover for the North campus; revoked early when the principal returned on 18 Aug.',
     TIMESTAMPTZ '2026-08-08 09:00:00+00', TIMESTAMPTZ '2026-08-18 04:00:00+00', 'anita.principal@dps.edu.in')
) AS d(tenant_id, delegator, delegatee, role_name, start_time, end_time, status, reason, created_at, updated_at, updated_by)
JOIN roles r ON r.name = d.role_name;

-- ---------------------------------------------------------------------------------------------------
-- 9. US-00008 Privacy anonymization (GDPR / DPDP) and US-00019 field-level encryption policies
-- ---------------------------------------------------------------------------------------------------
INSERT INTO privacy_anonymization_logs (tenant_id, user_id, profile_type, request_type, anonymized_fields, reason, status, executed_by,
                                        executed_at, created_at, updated_at, created_by, updated_by, version)
VALUES
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0001-0001-4000-8000-000000000015', 'STAFF', 'ANONYMIZATION',
 'phone,address,personal_email,bank_account_number,emergency_contact',
 'DPDP erasure request from former teacher after full and final settlement (HR ticket HR-2026-0918).', 'COMPLETED',
 'shalini.mehra@dps.edu.in', '2026-10-01 06:30:00+00', '2026-10-01 06:30:00+00', '2026-10-01 06:30:00+00', 'seed:module01', NULL, 0),
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'STU-2019-0457', 'STUDENT', 'ANONYMIZATION',
 'first_name,last_name,email,phone,address,guardian_details,date_of_birth',
 'Alumni record past the 5-year retention window (batch of 2019); academic results kept in aggregate form.', 'COMPLETED',
 'shalini.mehra@dps.edu.in', '2026-09-15 08:00:00+00', '2026-09-15 08:00:00+00', '2026-09-15 08:00:00+00', 'seed:module01', NULL, 0),
('b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'GRD-2021-1132', 'GUARDIAN', 'ANONYMIZATION',
 'email,phone,address,occupation,annual_income',
 'Guardian withdrew consent for data processing after the child transferred to another school.', 'COMPLETED',
 'mahesh.iyer@svkm.org', '2026-08-28 10:20:00+00', '2026-08-28 10:20:00+00', '2026-08-28 10:20:00+00', 'seed:module01', NULL, 0);

INSERT INTO field_encryption_policies (tenant_id, entity_name, encrypted_fields, algorithm, is_enforced, created_at, updated_at, created_by, updated_by, version)
VALUES
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'StudentHealthRecord', 'blood_group,allergies,medical_conditions,emergency_medication', 'AES-256-GCM', true,
 '2026-05-10 05:00:00+00', '2026-05-10 05:00:00+00', 'seed:module01', NULL, 0),
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'GuardianProfile', 'aadhaar_number,pan_number,annual_income', 'AES-256-GCM', true,
 '2026-05-10 05:05:00+00', '2026-05-10 05:05:00+00', 'seed:module01', NULL, 0),
('b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'StaffPayroll', 'bank_account_number,ifsc_code,gross_salary,pf_number', 'AES-256-GCM', true,
 '2026-05-22 06:00:00+00', '2026-05-22 06:00:00+00', 'seed:module01', NULL, 0),
('5eed0001-0000-4000-8000-000000000003', 'StudentHealthRecord', 'blood_group,allergies,medical_conditions', 'AES-256-GCM', false,
 '2026-06-05 05:00:00+00', '2026-06-05 05:00:00+00', 'seed:module01', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 10. US-00020 Security alert rules
-- ---------------------------------------------------------------------------------------------------
INSERT INTO security_alert_rules (tenant_id, rule_name, condition_pattern, threshold_attempts, severity, notification_channel, is_enabled,
                                  created_at, updated_at, created_by, updated_by, version)
VALUES
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Repeated failed logins on staff portal', 'EXCESSIVE_FAILED_LOGINS', 5, 'HIGH',     'EMAIL',   true,
 '2026-05-12 05:00:00+00', '2026-05-12 05:00:00+00', 'seed:module01', NULL, 0),
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Admin login from an unrecognised location', 'UNKNOWN_IP_LOCATION', 1, 'CRITICAL', 'SMS',     true,
 '2026-05-12 05:05:00+00', '2026-05-12 05:05:00+00', 'seed:module01', NULL, 0),
('b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Brute-force attempts on the parent app',    'EXCESSIVE_FAILED_LOGINS', 10, 'MEDIUM',  'WEBHOOK', true,
 '2026-05-25 06:00:00+00', '2026-05-25 06:00:00+00', 'seed:module01', NULL, 0),
('5eed0001-0000-4000-8000-000000000003', 'Off-campus admin login (pilot)',          'UNKNOWN_IP_LOCATION', 1, 'HIGH',     'EMAIL',   false,
 '2026-06-05 05:10:00+00', '2026-06-05 05:10:00+00', 'seed:module01', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 11. US-00010 Disaster recovery backups
-- ---------------------------------------------------------------------------------------------------
INSERT INTO disaster_recovery_backups (tenant_id, snapshot_id, backup_type, storage_provider, cloud_bucket, encryption_algorithm, size_bytes,
                                       backup_timestamp, status, created_at, updated_at, created_by, updated_by, version)
VALUES
('a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'snap-dps-inc-' || to_char(now() - interval '1 day', 'YYYYMMDD'), 'INCREMENTAL', 'AWS_S3',
 's3://schoolerp-dps-backups/incremental/', 'AES-256-GCM', 187695104, date_trunc('day', now() - interval '1 day') + interval '20 hours 30 minutes', 'COMPLETED',
 now() - interval '1 day', now() - interval '1 day', 'seed:module01', NULL, 0),
('b1eEBc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'snap-svkm-full-20261012', 'FULL', 'AWS_S3',
 's3://schoolerp-svkm-backups/snapshots/', 'AES-256-GCM', 2362232012, '2026-10-12 21:00:00+00', 'COMPLETED',
 '2026-10-12 21:00:00+00', '2026-10-12 22:10:00+00', 'seed:module01', NULL, 0),
('5eed0001-0000-4000-8000-000000000003', 'snap-gmet-full-20261001', 'FULL', 'AWS_S3',
 's3://schoolerp-gmet-backups-ap-south-1/snapshots/', 'AES-256-GCM', 734003200, '2026-10-01 20:30:00+00', 'COMPLETED',
 '2026-10-01 20:30:00+00', '2026-10-01 21:05:00+00', 'seed:module01', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 12. US-00005 / US-00015 Audit trail for the seeded security events
-- ---------------------------------------------------------------------------------------------------
INSERT INTO audit_logs (user_id, ip_address, action, resource, outcome, event_time, details, created_at, updated_at, created_by, updated_by, version)
SELECT a.user_id, a.ip, a.action, a.resource, a.outcome, a.at, a.details, a.at, a.at, 'seed:module01', NULL, 0
FROM (VALUES
    ('5eed0001-0001-4000-8000-000000000021', '10.30.4.21',  'REGISTER_SUPER_ADMIN', 'tenants', 'SUCCESS', TIMESTAMPTZ '2026-06-02 05:30:00+00',
     'Registered super admin superadmin_lavanya for Greenfield Montessori Education Trust (GMET-SOC-03)'),
    ('5eed0001-0001-4000-8000-000000000021', '10.30.4.21',  'PROVISION_BRANCH', 'branches_provisioning', 'SUCCESS', TIMESTAMPTZ '2026-06-02 06:00:00+00',
     'Provisioned branch GMET-BLR-WHITEFIELD (IB) with isolated schema tenant_gmet_whitefield'),
    ('5eed0001-0001-4000-8000-000000000002', '10.20.1.5',   'UPDATE_SESSION_CONFIG', 'session_config', 'SUCCESS', TIMESTAMPTZ '2026-08-18 05:00:00+00',
     'Idle timeout set to 15 minutes and campus subnet enforcement reviewed for DPS North'),
    ('5eed0001-0001-4000-8000-000000000016', '10.20.2.17',  'LOGIN_FAILURE', 'user_credentials', 'FAILURE', now() - interval '9 minutes',
     'Failed password attempt (4/5) for user: teacher_farhan'),
    ('5eed0001-0001-4000-8000-000000000016', '10.20.2.17',  'LOGIN_FAILURE', 'user_credentials', 'FAILURE', now() - interval '5 minutes',
     'Failed password attempt (5/5) for user: teacher_farhan'),
    ('5eed0001-0001-4000-8000-000000000016', '10.20.2.17',  'LOGIN_LOCKED', 'user_credentials', 'FAILURE', now() - interval '4 minutes',
     'Login rejected. Account is locked for 30 minutes after 5 consecutive failed attempts'),
    ('5eed0001-0001-4000-8000-000000000009', '10.20.1.15',  'LOGIN_SUCCESS', 'user_sessions', 'SUCCESS', now() - interval '2 hours',
     'User ''counselor_meera'' logged in successfully from IP: 10.20.1.15'),
    ('5eed0001-0001-4000-8000-000000000008', '10.20.1.22',  'LOGIN_SUCCESS', 'user_sessions', 'SUCCESS', now() - interval '3 hours',
     'User ''admissions_deepak'' logged in successfully from IP: 10.20.1.22'),
    ('5eed0001-0001-4000-8000-000000000004', '10.20.1.33',  'REVOKE_USER_SESSIONS', 'user_sessions', 'SUCCESS', TIMESTAMPTZ '2026-09-30 12:00:00+00',
     'Revoked 2 active sessions of teacher_neeraj after resignation; account deactivated'),
    ('5eed0001-0001-4000-8000-000000000006', '10.20.1.44',  'ANONYMIZE_PROFILE', 'privacy_anonymization_logs', 'SUCCESS', TIMESTAMPTZ '2026-10-01 06:30:00+00',
     'Anonymized STAFF profile 5eed0001-0001-4000-8000-000000000015 (phone, address, personal_email, bank_account_number, emergency_contact)'),
    ('5eed0001-0001-4000-8000-000000000005', '10.20.1.48',  'REVOKE_API_TOKEN', 'api_tokens', 'SUCCESS', now() - interval '5 days',
     'Revoked API token ''Library RFID Gate Reader'' at expiry; replacement scheduled with the new RFID vendor'),
    ('5eed0001-0001-4000-8000-000000000004', '10.20.1.33',  'CREATE_ROLE_DELEGATION', 'role_delegations', 'SUCCESS', now() - interval '2 days',
     'Delegated DEPARTMENT_HEAD to teacher_vikram for 7 days'),
    ('5eed0001-0001-4000-8000-000000000018', '10.40.1.3',   'EXECUTE_BACKUP', 'disaster_recovery_backups', 'SUCCESS', TIMESTAMPTZ '2026-10-12 22:10:00+00',
     'FULL backup snap-svkm-full-20261012 completed (2.2 GB, AES-256-GCM, AWS_S3)')
) AS a(user_id, ip, action, resource, outcome, at, details);

COMMIT;
