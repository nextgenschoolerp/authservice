package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MfaConfigResponse {
    private Long id;
    private String tenantId;
    private boolean totpEnabled;
    private boolean smsEnabled;
    private boolean mandatoryForSensitiveTransactions;
    private int maxFailedAttempts;
}
