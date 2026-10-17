package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores automated security anomaly alert rules (e.g. login attempts from unknown locations).
 * Covers: US-00020, US-00040, US-00060, US-00080, US-00100
 * Actor: IT System Admin
 */
@Entity
@Table(name = "security_alert_rules")
@Getter
@Setter
public class SecurityAlertRule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Column(name = "condition_pattern", nullable = false, length = 100)
    private String conditionPattern; // e.g. UNKNOWN_IP_LOCATION

    @Column(name = "threshold_attempts", nullable = false)
    private int thresholdAttempts = 3;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity = "HIGH";

    @Column(name = "notification_channel", nullable = false, length = 100)
    private String notificationChannel = "EMAIL";

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;
}
