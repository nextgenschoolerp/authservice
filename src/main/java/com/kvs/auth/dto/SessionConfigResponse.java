package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionConfigResponse {
    private Long id;
    private String tenantId;
    private int idleTimeoutMinutes;
    private int maxSessionDurationMinutes;
    private boolean concurrentSessionsAllowed;
    private int maxConcurrentSessions;
}
