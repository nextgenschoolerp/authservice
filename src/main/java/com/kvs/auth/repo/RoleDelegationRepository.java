package com.kvs.auth.repo;

import com.kvs.auth.entity.RoleDelegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleDelegationRepository extends JpaRepository<RoleDelegation, Long> {
    List<RoleDelegation> findByTenantId(String tenantId);
    List<RoleDelegation> findByTenantIdAndDelegatorUserId(String tenantId, String delegatorUserId);
    List<RoleDelegation> findByTenantIdAndDelegateeUserId(String tenantId, String delegateeUserId);
}
