package com.kvs.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlertRuleRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @NotBlank(message = "Condition is required")
    private String condition; // e.g. UNKNOWN_IP_LOCATION, EXCESSIVE_FAILED_LOGINS

    @Min(value = 1, message = "Threshold attempts must be at least 1")
    @Max(value = 50, message = "Threshold attempts cannot exceed 50")
    private int thresholdAttempts;

    @NotBlank(message = "Severity is required")
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @NotBlank(message = "Notification channel is required")
    private String notificationChannel; // EMAIL, SMS, WEBHOOK

    private boolean enabled;
}
