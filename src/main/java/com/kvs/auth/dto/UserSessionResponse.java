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
public class UserSessionResponse {

    private String sessionId;
    private String userId;
    private String tenantId;
    private String ipAddress;
    private String userAgent;
    private Instant loginAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private boolean revoked;
}
