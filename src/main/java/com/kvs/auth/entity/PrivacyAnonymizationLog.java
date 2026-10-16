package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores privacy anonymization logs for GDPR and DPDP compliance.
 * Covers: US-00008 — Anonymize inactive student and staff profiles to comply with privacy laws
 * Actors: Compliance Officer
 */
@Entity
@Table(name = "privacy_anonymization_logs")
@Getter
@Setter
public class PrivacyAnonymizationLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "profile_type", nullable = false, length = 50)
    private String profileType; // e.g., STUDENT, STAFF

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType; // e.g., RIGHT_TO_BE_FORGOTTEN, ANONYMIZATION

    @Column(name = "anonymized_fields", length = 500)
    private String anonymizedFields; // e.g., "name,email,phone,address"

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "COMPLETED";

    @Column(name = "executed_by", nullable = false, length = 100)
    private String executedBy;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();
}
