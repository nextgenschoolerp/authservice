package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordPolicyResponse {

    private String tenantId;
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigits;
    private boolean requireSpecialChars;
    private int expiryDays;
    private int preventReuseCount;
    private String status;
}
