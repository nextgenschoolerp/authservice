package com.kvs.auth.repo;

import com.kvs.auth.entity.TenantIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantIntegrationRepository extends JpaRepository<TenantIntegration, Long> {
    List<TenantIntegration> findByTenantId(String tenantId);
    Optional<TenantIntegration> findByTenantIdAndChannel(String tenantId, String channel);
}
