package com.kvs.auth.repo;

import com.kvs.auth.entity.MfaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaConfigRepository extends JpaRepository<MfaConfig, Long> {
    Optional<MfaConfig> findByTenantId(String tenantId);
}
