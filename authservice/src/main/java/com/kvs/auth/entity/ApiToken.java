package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores encrypted/hashed OAuth2 API tokens for third-party integrations.
 * Covers: US-00007 — Generate encrypted oauth2 api tokens for third-party software integrations
 * Actors: Admin Clerk
 */
@Entity
@Table(name = "api_tokens")
@Getter
@Setter
public class ApiToken extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "token_name", nullable = false, length = 100)
    private String tokenName;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "scopes", nullable = false, length = 255)
    private String scopes;

    @Column(name = "rate_limit_rpm", nullable = false)
    private int rateLimitRpm = 120;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
