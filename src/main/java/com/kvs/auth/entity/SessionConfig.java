package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores global session and idle timeout security configuration.
 * Covers: US-00004 — Set global session idle timeout thresholds to 15 minutes
 * Actors: IT System Admin
 */
@Entity
@Table(name = "session_config")
@Getter
@Setter
public class SessionConfig extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant this config applies to (null = global) */
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    /** Idle timeout in minutes before session is invalidated (default: 15) */
    @Column(name = "idle_timeout_minutes", nullable = false, columnDefinition = "integer not null default 15")
    private int idleTimeoutMinutes = 15;

    /** Maximum absolute session duration in minutes */
    @Column(name = "max_session_duration_minutes", nullable = false, columnDefinition = "integer not null default 480")
    private int maxSessionDurationMinutes = 480;

    /** Whether concurrent sessions from the same user are allowed */
    @Column(name = "concurrent_sessions_allowed", nullable = false, columnDefinition = "boolean not null default false")
    private boolean concurrentSessionsAllowed = false;

    /** Maximum number of concurrent sessions per user */
    @Column(name = "max_concurrent_sessions", nullable = false, columnDefinition = "integer not null default 1")
    private int maxConcurrentSessions = 1;

    /** Authorized campus IP subnets or CIDRs (comma-delimited) */
    @Column(name = "allowed_subnets", length = 1000)
    private String allowedSubnets;

    /** Whether login is restricted strictly to campus subnets */
    @Column(name = "enforce_campus_subnet", nullable = false, columnDefinition = "boolean not null default false")
    private boolean enforceCampusSubnet = false;

    /** Maximum consecutive failed password attempts before lockout (default: 5) */
    @Column(name = "max_failed_attempts", nullable = false, columnDefinition = "integer not null default 5")
    private int maxFailedAttempts = 5;

    /** Lockout duration in minutes (default: 30) */
    @Column(name = "lockout_duration_minutes", nullable = false, columnDefinition = "integer not null default 30")
    private int lockoutDurationMinutes = 30;

    /** Whether account automatically unlocks after duration expires */
    @Column(name = "auto_unlock_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean autoUnlockEnabled = true;

    /** Password complexity: minimum character length */
    @Column(name = "min_password_length", nullable = false, columnDefinition = "integer not null default 8")
    private int minPasswordLength = 8;

    /** Password complexity: require uppercase letter */
    @Column(name = "require_uppercase", nullable = false, columnDefinition = "boolean not null default true")
    private boolean requireUppercase = true;

    /** Password complexity: require lowercase letter */
    @Column(name = "require_lowercase", nullable = false, columnDefinition = "boolean not null default true")
    private boolean requireLowercase = true;

    /** Password complexity: require numeric digit */
    @Column(name = "require_digits", nullable = false, columnDefinition = "boolean not null default true")
    private boolean requireDigits = true;

    /** Password complexity: require special character */
    @Column(name = "require_special_chars", nullable = false, columnDefinition = "boolean not null default true")
    private boolean requireSpecialChars = true;

    /** Password expiry period in days (0 = never) */
    @Column(name = "password_expiry_days", nullable = false, columnDefinition = "integer not null default 90")
    private int passwordExpiryDays = 90;

    /** Number of past passwords prevented from reuse */
    @Column(name = "prevent_reuse_count", nullable = false, columnDefinition = "integer not null default 3")
    private int preventReuseCount = 3;
}
