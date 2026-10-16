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
    @Column(name = "idle_timeout_minutes", nullable = false)
    private int idleTimeoutMinutes = 15;

    /** Maximum absolute session duration in minutes */
    @Column(name = "max_session_duration_minutes", nullable = false)
    private int maxSessionDurationMinutes = 480;

    /** Whether concurrent sessions from the same user are allowed */
    @Column(name = "concurrent_sessions_allowed", nullable = false)
    private boolean concurrentSessionsAllowed = false;

    /** Maximum number of concurrent sessions per user */
    @Column(name = "max_concurrent_sessions", nullable = false)
    private int maxConcurrentSessions = 1;
}
