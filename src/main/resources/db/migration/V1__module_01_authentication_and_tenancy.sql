-- ============================================================================
-- SCHOOL ERP ENTERPRISE PLATFORM - MODULE 01 DATABASE SCHEMA
-- Module 01: Authentication, Access Control & Multi-Tenant Infrastructure
-- Database Engine: PostgreSQL 14+
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. ENUM DEFINITIONS
-- ============================================================================

CREATE TYPE user_type_enum AS ENUM (
    'SUPER_ADMIN',
    'TRUSTEE',
    'PRINCIPAL',
    'TEACHER',
    'FINANCE_OFFICER',
    'HR_MANAGER',
    'PARENT',
    'STUDENT',
    'OPERATIONAL_STAFF'
);

CREATE TYPE tenant_status_enum AS ENUM (
    'ACTIVE',
    'SUSPENDED',
    'PENDING_ONBOARDING',
    'ARCHIVED'
);

CREATE TYPE mfa_type_enum AS ENUM (
    'TOTP',
    'SMS',
    'EMAIL'
);

CREATE TYPE audit_action_enum AS ENUM (
    'CREATE',
    'READ',
    'UPDATE',
    'DELETE',
    'LOGIN_SUCCESS',
    'LOGIN_FAILURE',
    'LOGOUT',
    'PASSWORD_CHANGE',
    'PERMISSION_CHANGE',
    'DELEGATION_GRANT',
    'DELEGATION_REVOKE',
    'ANONYMIZATION'
);

CREATE TYPE delegation_status_enum AS ENUM (
    'ACTIVE',
    'EXPIRED',
    'REVOKED',
    'PENDING'
);

-- ============================================================================
-- 2. TRIGGER FUNCTION FOR UPDATED_AT TIMESTAMPS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. CORE MULTI-TENANT & SYSTEM TABLES
-- ============================================================================

-- 3.1 Tenants (Trusts / Societies)
CREATE TABLE tenants (
    tenant_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    society_code VARCHAR(30) UNIQUE NOT NULL,
    society_name VARCHAR(255) NOT NULL,
    domain_name VARCHAR(255) UNIQUE,
    status tenant_status_enum NOT NULL DEFAULT 'PENDING_ONBOARDING',
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3.2 Branches / Campuses
CREATE TABLE branches (
    branch_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    branch_code VARCHAR(30) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    affiliation_board VARCHAR(100) NOT NULL, -- e.g., CBSE, ICSE, State Board
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_branch_code UNIQUE (tenant_id, branch_code)
);

CREATE TRIGGER trg_branches_updated_at
    BEFORE UPDATE ON branches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3.3 Tenant Subscription Licenses & Capacity Caps
CREATE TABLE tenant_licenses (
    license_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    plan_tier VARCHAR(50) NOT NULL, -- e.g., BASIC, PREMIUM, PROFESSIONAL
    max_students_cap INT NOT NULL DEFAULT 500,
    max_branches_cap INT NOT NULL DEFAULT 1,
    storage_limit_gb INT NOT NULL DEFAULT 50,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 4. RBAC & PERMISSION TABLES
-- ============================================================================

-- 4.1 Roles (System & Custom Roles)
CREATE TABLE roles (
    role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(tenant_id) ON DELETE CASCADE, -- NULL for system-wide default roles
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    is_custom BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_role_code UNIQUE (tenant_id, role_code)
);

-- 4.2 Granular Permissions Matrix
CREATE TABLE permissions (
    permission_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_code VARCHAR(50) NOT NULL, -- e.g., AUTH, SIS, FINANCE, EXAMS
    sub_module VARCHAR(100) NOT NULL,
    screen_code VARCHAR(100) NOT NULL,
    action_code VARCHAR(50) NOT NULL, -- READ, WRITE, DELETE, EXPORT, APPROVE
    permission_name VARCHAR(150) NOT NULL,
    description TEXT,
    CONSTRAINT uq_permission_action UNIQUE (module_code, screen_code, action_code)
);

-- 4.3 Role-Permissions Mapping
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================================
-- 5. USER ACCOUNTS & SECURITY CREDENTIALS
-- ============================================================================

-- 5.1 Users Master Table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(branch_id) ON DELETE SET NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    user_type user_type_enum NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT uq_tenant_email UNIQUE (tenant_id, email)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 5.2 User Assigned Roles Mapping
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- 5.3 User Credentials & Lockout Logic
CREATE TABLE user_credentials (
    credential_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE
);

-- 5.4 MFA Configurations
CREATE TABLE mfa_configurations (
    mfa_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    mfa_type mfa_type_enum NOT NULL DEFAULT 'TOTP',
    secret_key_encrypted TEXT NOT NULL,
    backup_codes_hash JSONB,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    configured_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 6. SSO & FEDERATED IDENTITY
-- ============================================================================

-- 6.1 Tenant SSO Identity Providers (SAML2 / OAuth2)
CREATE TABLE sso_providers (
    provider_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    provider_name VARCHAR(100) NOT NULL, -- e.g., GOOGLE_WORKSPACE, AZURE_AD, OKTA
    protocol_type VARCHAR(20) NOT NULL CHECK (protocol_type IN ('SAML2', 'OAUTH2', 'OIDC')),
    client_id VARCHAR(255) NOT NULL,
    client_secret_encrypted TEXT,
    issuer_url VARCHAR(255) NOT NULL,
    sso_url VARCHAR(255) NOT NULL,
    certificate_pem TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6.2 User SSO External Identities
CREATE TABLE user_sso_identities (
    identity_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES sso_providers(provider_id) ON DELETE CASCADE,
    external_subject_id VARCHAR(255) NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_external_id UNIQUE (provider_id, external_subject_id)
);

-- ============================================================================
-- 7. SESSION MANAGEMENT, IP SECURITY & API TOKENS
-- ============================================================================

-- 7.1 IP Subnet Whitelisting Rules
CREATE TABLE ip_whitelists (
    rule_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(branch_id) ON DELETE CASCADE,
    ip_cidr_range VARCHAR(50) NOT NULL, -- e.g., 192.168.1.0/24
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7.2 Active User Sessions
CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    session_token_hash VARCHAR(255) NOT NULL UNIQUE,
    ip_address INET NOT NULL,
    user_agent TEXT,
    device_fingerprint VARCHAR(255),
    login_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE
);

-- 7.3 Developer & Integration API Tokens
CREATE TABLE api_tokens (
    token_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    scopes_json JSONB NOT NULL, -- e.g., ["read:students", "write:fees"]
    rate_limit_rpm INT NOT NULL DEFAULT 120, -- requests per minute
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 8. GOVERNANCE: DELEGATIONS, AUDIT LOGS & PRIVACY
-- ============================================================================

-- 8.1 Temporary Authority Delegations
CREATE TABLE role_delegations (
    delegation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    delegator_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    delegatee_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT NOT NULL,
    status delegation_status_enum NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_delegation_times CHECK (end_time > start_time)
);

-- 8.2 Comprehensive System Audit Logs
CREATE TABLE audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    action_type audit_action_enum NOT NULL,
    module_code VARCHAR(50) NOT NULL,
    target_entity VARCHAR(100) NOT NULL,
    target_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    details_jsonb JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8.3 GDPR / DPDP Privacy & Anonymization Audit
CREATE TABLE privacy_anonymization_logs (
    request_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    user_id UUID NOT NULL, -- soft link in case user record is scrubbed
    request_type VARCHAR(50) NOT NULL, -- e.g., RIGHT_TO_FORGET, DATA_EXPORT
    anonymized_fields_json JSONB,
    executed_by_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 9. PERFORMANCE INDEXING STRATEGY
-- ============================================================================

-- Foreign Key B-Tree Indexes
CREATE INDEX idx_branches_tenant_id ON branches(tenant_id);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_branch_id ON users(branch_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_perm ON role_permissions(permission_id);

-- Session & Security Indexes
CREATE INDEX idx_user_sessions_token ON user_sessions(session_token_hash);
CREATE INDEX idx_user_sessions_lookup ON user_sessions(user_id, is_revoked, expires_at);
CREATE INDEX idx_api_tokens_lookup ON api_tokens(token_hash, is_active);
CREATE INDEX idx_ip_whitelists_tenant ON ip_whitelists(tenant_id, is_active);

-- Audit Trail & Time-Series Indexes
CREATE INDEX idx_audit_logs_tenant_date ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_user_date ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action_type);
CREATE INDEX idx_audit_logs_details_gin ON audit_logs USING GIN (details_jsonb);

-- Partial Indexes for Performance
CREATE INDEX idx_users_active_tenant ON users(tenant_id, user_id) WHERE is_active = TRUE;
CREATE INDEX idx_roles_custom ON roles(tenant_id) WHERE is_custom = TRUE;

-- ============================================================================
-- 10. ROW LEVEL SECURITY (RLS) MULTI-TENANCY POLICIES
-- ============================================================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE branches ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Tenant Isolation RLS Policies (Requires application to set 'app.current_tenant_id')
CREATE POLICY tenant_isolation_users ON users
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY tenant_isolation_branches ON branches
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY tenant_isolation_sessions ON user_sessions
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

CREATE POLICY tenant_isolation_audit ON audit_logs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);

-- ============================================================================
