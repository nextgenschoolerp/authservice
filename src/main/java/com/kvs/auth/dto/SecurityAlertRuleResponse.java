package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlertRuleResponse {

    private Long id;
    private String tenantId;
    private String ruleName;
    private String condition;
    private int thresholdAttempts;
    private String severity;
    private String notificationChannel;
    private boolean enabled;
    private Instant createdAt;
    private String status;
}
