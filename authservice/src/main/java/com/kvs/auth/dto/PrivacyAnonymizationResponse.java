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
public class PrivacyAnonymizationResponse {
    private Long id;
    private String tenantId;
    private String userId;
    private String profileType;
    private String requestType;
    private String anonymizedFields;
    private String reason;
    private String status;
    private String executedBy;
    private Instant executedAt;
}
