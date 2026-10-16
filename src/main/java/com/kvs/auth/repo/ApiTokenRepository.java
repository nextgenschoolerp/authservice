package com.kvs.auth.repo;

import com.kvs.auth.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    List<ApiToken> findByTenantId(String tenantId);
    Optional<ApiToken> findByTokenHash(String tokenHash);
}
