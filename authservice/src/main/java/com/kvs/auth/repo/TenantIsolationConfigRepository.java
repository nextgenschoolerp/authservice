package com.kvs.auth.repo;

import com.kvs.auth.entity.TenantIsolationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantIsolationConfigRepository extends JpaRepository<TenantIsolationConfig, Long> {
    Optional<TenantIsolationConfig> findByTenantId(String tenantId);
}
