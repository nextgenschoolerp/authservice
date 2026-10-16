package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores automated database snapshot backup records to encrypted cloud storage.
 * Covers: US-00010 — Execute automated database snapshot backups to encrypted cloud storage
 * Actors: Trustee
 */
@Entity
@Table(name = "disaster_recovery_backups")
@Getter
@Setter
public class DisasterRecoveryBackup extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "snapshot_id", nullable = false, unique = true, length = 100)
    private String snapshotId;

    @Column(name = "backup_type", nullable = false, length = 50)
    private String backupType; // e.g. FULL, INCREMENTAL, SNAPSHOT

    @Column(name = "storage_provider", nullable = false, length = 50)
    private String storageProvider; // e.g. AWS_S3, AZURE_BLOB, GCP_STORAGE

    @Column(name = "cloud_bucket", nullable = false, length = 255)
    private String cloudBucket;

    @Column(name = "encryption_algorithm", nullable = false, length = 50)
    private String encryptionAlgorithm; // e.g. AES-256-GCM

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "COMPLETED";

    @Column(name = "backup_timestamp", nullable = false)
    private Instant backupTimestamp = Instant.now();
}
