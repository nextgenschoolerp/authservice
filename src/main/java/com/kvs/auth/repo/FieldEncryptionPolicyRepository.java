package com.kvs.auth.repo;

import com.kvs.auth.entity.FieldEncryptionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldEncryptionPolicyRepository extends JpaRepository<FieldEncryptionPolicy, Long> {

    Optional<FieldEncryptionPolicy> findByTenantIdAndEntityName(String tenantId, String entityName);

    List<FieldEncryptionPolicy> findByTenantId(String tenantId);
}
