package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockoutPolicyResponse {

    private String tenantId;
    private int maxFailedAttempts;
    private int lockoutDurationMinutes;
    private boolean autoUnlockEnabled;
    private String status;
}
