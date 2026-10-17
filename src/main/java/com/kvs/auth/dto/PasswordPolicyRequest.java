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
public class PasswordPolicyRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @Min(value = 8, message = "Minimum password length must be at least 8 characters")
    @Max(value = 64, message = "Minimum password length cannot exceed 64 characters")
    private int minLength;

    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigits;
    private boolean requireSpecialChars;

    @Min(value = 0, message = "Password expiry days must be non-negative")
    private int expiryDays;

    @Min(value = 0, message = "Prevent reuse count must be non-negative")
    private int preventReuseCount;
}
