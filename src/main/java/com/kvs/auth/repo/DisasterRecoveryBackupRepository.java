package com.kvs.auth.repo;

import com.kvs.auth.entity.DisasterRecoveryBackup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisasterRecoveryBackupRepository extends JpaRepository<DisasterRecoveryBackup, Long> {
    List<DisasterRecoveryBackup> findByTenantId(String tenantId);
    Optional<DisasterRecoveryBackup> findBySnapshotId(String snapshotId);
}
