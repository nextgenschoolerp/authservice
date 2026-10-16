package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MfaConfigRequest {
    private String tenantId;
    private boolean totpEnabled;
    private boolean smsEnabled;
    private boolean mandatoryForSensitiveTransactions;
    private int maxFailedAttempts;
}
