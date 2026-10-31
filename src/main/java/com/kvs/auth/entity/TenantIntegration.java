package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One school's connection to an outside provider (Settings → Integrations): EMAIL (SMTP), SMS (MSG91 or Twilio),
 * WHATSAPP (Meta Cloud API) or PAYMENT. Plain settings are JSON; the provider secret (password, API key, token) is stored
 * only encrypted (AES-GCM) and is never returned by the API.
 */
@Entity
@Table(name = "tenant_integrations", uniqueConstraints = @UniqueConstraint(name = "uk_tenant_integration_channel", columnNames = {"tenant_id", "channel"}))
@Getter
@Setter
public class TenantIntegration extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /** EMAIL, SMS, WHATSAPP or PAYMENT */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /** SMTP, MSG91, TWILIO, META_CLOUD … */
    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

    /** Encrypted secret: base64(iv ‖ ciphertext); null when none is saved */
    @Column(name = "secret_cipher", columnDefinition = "TEXT")
    private String secretCipher;

    /** Last 4 characters of the secret, to show "••••abcd" */
    @Column(name = "secret_hint", length = 8)
    private String secretHint;

    /** SAVED (not tested since the last change), VERIFIED or FAILED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "last_test_at")
    private Instant lastTestAt;

    @Column(name = "last_test_result", length = 500)
    private String lastTestResult;
}
