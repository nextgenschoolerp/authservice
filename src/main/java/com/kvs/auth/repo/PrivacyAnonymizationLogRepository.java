package com.kvs.auth.repo;

import com.kvs.auth.entity.PrivacyAnonymizationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivacyAnonymizationLogRepository extends JpaRepository<PrivacyAnonymizationLog, Long> {
    List<PrivacyAnonymizationLog> findByTenantId(String tenantId);
    List<PrivacyAnonymizationLog> findByTenantIdAndUserId(String tenantId, String userId);
}
