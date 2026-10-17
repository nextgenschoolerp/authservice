package com.kvs.auth.repo;

import com.kvs.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantId(String tenantId);

    Optional<Tenant> findBySocietyCode(String societyCode);

    boolean existsBySocietyCode(String societyCode);

    boolean existsByDomainName(String domainName);
}
