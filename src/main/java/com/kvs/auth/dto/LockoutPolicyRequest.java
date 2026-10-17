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
public class LockoutPolicyRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @Min(value = 3, message = "Max failed attempts must be at least 3")
    @Max(value = 10, message = "Max failed attempts cannot exceed 10")
    private int maxFailedAttempts;

    @Min(value = 5, message = "Lockout duration must be at least 5 minutes")
    @Max(value = 1440, message = "Lockout duration cannot exceed 24 hours")
    private int lockoutDurationMinutes;

    private boolean autoUnlockEnabled;
}
