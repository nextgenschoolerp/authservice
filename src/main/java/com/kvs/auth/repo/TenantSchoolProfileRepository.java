package com.kvs.auth.repo;

import com.kvs.auth.entity.TenantSchoolProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSchoolProfileRepository extends JpaRepository<TenantSchoolProfile, Long> {

    Optional<TenantSchoolProfile> findByTenantId(String tenantId);
}
