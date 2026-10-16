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
public class TenantIsolationConfigResponse {
    private Long id;
    private String tenantId;
    private String schemaName;
    private boolean crossTenantAccessBlocked;
    private String encryptionAlgorithm;
    private boolean rowLevelSecurityEnabled;
    private Instant updatedAt;
}
