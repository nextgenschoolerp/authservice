package com.kvs.auth.repo;

import com.kvs.auth.entity.SessionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionConfigRepository extends JpaRepository<SessionConfig, Long> {
    Optional<SessionConfig> findByTenantId(String tenantId);
}
