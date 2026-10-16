package com.kvs.auth.repo;

import com.kvs.auth.entity.BranchProvisioning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchProvisioningRepository extends JpaRepository<BranchProvisioning, Long> {
    List<BranchProvisioning> findByTenantId(String tenantId);
    Optional<BranchProvisioning> findByTenantIdAndBranchCode(String tenantId, String branchCode);
    Optional<BranchProvisioning> findBySchemaName(String schemaName);
}
