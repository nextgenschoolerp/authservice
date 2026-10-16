package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisasterRecoveryBackupRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Backup type is required (e.g. FULL, INCREMENTAL, SNAPSHOT)")
    private String backupType;

    @NotBlank(message = "Storage provider is required (e.g. AWS_S3, AZURE_BLOB, GCP_STORAGE)")
    private String storageProvider;

    @NotBlank(message = "Cloud bucket is required")
    private String cloudBucket;

    @NotBlank(message = "Encryption algorithm is required (e.g. AES-256-GCM)")
    private String encryptionAlgorithm;
}
