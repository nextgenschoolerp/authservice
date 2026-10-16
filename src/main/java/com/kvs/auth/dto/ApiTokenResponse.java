package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiTokenResponse {
    private Long id;
    private String tenantId;
    private String userId;
    private String tokenName;
    private String tokenValue; // Returned only on generation
    private String scopes;
    private int rateLimitRpm;
    private Instant expiresAt;
    private boolean active;
}
