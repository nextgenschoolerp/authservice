package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldEncryptionPolicyRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Entity name is required")
    private String entityName;

    @NotEmpty(message = "Encrypted fields list cannot be empty")
    private List<String> encryptedFields;

    @NotBlank(message = "Encryption algorithm is required")
    private String algorithm;

    private boolean enforced;
}
