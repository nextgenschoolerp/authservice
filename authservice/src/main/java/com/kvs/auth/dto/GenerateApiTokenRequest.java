package com.kvs.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateApiTokenRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Token name is required")
    private String tokenName;

    @NotBlank(message = "Scopes are required")
    private String scopes; // e.g. "read:students,write:attendance"

    @Min(value = 10, message = "Rate limit must be at least 10 rpm")
    @Max(value = 10000, message = "Rate limit cannot exceed 10000 rpm")
    private int rateLimitRpm = 120;

    @Min(value = 1, message = "Validity days must be at least 1")
    private int validityDays = 30;
}
