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
public class DisasterRecoveryBackupResponse {
    private Long id;
    private String tenantId;
    private String snapshotId;
    private String backupType;
    private String storageProvider;
    private String cloudBucket;
    private String encryptionAlgorithm;
    private Long sizeBytes;
    private String status;
    private Instant backupTimestamp;
}
