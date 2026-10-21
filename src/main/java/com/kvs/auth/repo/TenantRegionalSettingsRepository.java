package com.kvs.auth.repo;

import com.kvs.auth.entity.TenantRegionalSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRegionalSettingsRepository extends JpaRepository<TenantRegionalSettings, Long> {
    Optional<TenantRegionalSettings> findByTenantId(String tenantId);
}
