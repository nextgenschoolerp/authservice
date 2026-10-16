package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores the global MFA policy configuration.
 * Covers: US-00003 — Enforce mandatory TOTP/SMS MFA for sensitive transactions
 * Actors: Principal
 */
@Entity
@Table(name = "mfa_config")
@Getter
@Setter
public class MfaConfig extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant this MFA config applies to (null = global) */
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    /** Whether TOTP-based MFA is enabled */
    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    /** Whether SMS-based MFA is enabled */
    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = false;

    /** Whether MFA is mandatory for all sensitive transactions */
    @Column(name = "mandatory_for_sensitive_transactions", nullable = false)
    private boolean mandatoryForSensitiveTransactions = true;

    /** Number of failed MFA attempts before account lockout */
    @Column(name = "max_failed_attempts", nullable = false)
    private int maxFailedAttempts = 5;
}
