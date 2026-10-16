package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantIsolationConfigRequest {

    @NotBlank(message = "Schema name is required")
    @Size(max = 100, message = "Schema name must not exceed 100 characters")
    private String schemaName;

    private boolean crossTenantAccessBlocked = true;

    @Size(max = 50, message = "Encryption algorithm must not exceed 50 characters")
    private String encryptionAlgorithm;

    private boolean rowLevelSecurityEnabled = true;
}
