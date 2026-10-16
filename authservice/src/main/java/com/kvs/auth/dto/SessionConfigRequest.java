package com.kvs.auth.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionConfigRequest {
    private String tenantId;

    @Min(value = 1, message = "Idle timeout must be at least 1 minute")
    private int idleTimeoutMinutes;

    @Min(value = 1, message = "Max session duration must be at least 1 minute")
    private int maxSessionDurationMinutes;

    private boolean concurrentSessionsAllowed;
    private int maxConcurrentSessions;
}
