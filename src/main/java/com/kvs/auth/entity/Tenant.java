package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores educational society or trust tenant details.
 * Foundational multi-tenant master entity.
 */
@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenants_society_code", columnList = "society_code", unique = true),
        @Index(name = "idx_tenants_domain_name", columnList = "domain_name", unique = true)
})
@Getter
@Setter
public class Tenant extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", unique = true, nullable = false, length = 100)
    private String tenantId;

    @Column(name = "society_code", unique = true, nullable = false, length = 50)
    private String societyCode;

    @Column(name = "society_name", nullable = false, length = 255)
    private String societyName;

    @Column(name = "domain_name", length = 255)
    private String domainName;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;
}
