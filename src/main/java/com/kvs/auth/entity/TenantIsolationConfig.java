package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores per-tenant database isolation configuration parameters.
 * Covers: US-00001 — Configure multi-tenant database isolation parameters
 * Actors: Super Admin
 */
@Entity
@Table(name = "tenant_isolation_config")
@Getter
@Setter
public class TenantIsolationConfig extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique tenant identifier (e.g., school branch code) */
    @Column(name = "tenant_id", unique = true, nullable = false, length = 100)
    private String tenantId;

    /** Database schema name assigned to this tenant */
    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    /** Whether cross-tenant data access is strictly blocked */
    @Column(name = "cross_tenant_access_blocked", nullable = false)
    private boolean crossTenantAccessBlocked = true;

    /** Encryption algorithm used for tenant data at rest */
    @Column(name = "encryption_algorithm", length = 50)
    private String encryptionAlgorithm;

    /** Whether row-level security is enabled for this tenant */
    @Column(name = "row_level_security_enabled", nullable = false)
    private boolean rowLevelSecurityEnabled = true;
}
