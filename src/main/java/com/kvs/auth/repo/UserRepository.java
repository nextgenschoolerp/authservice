package com.kvs.auth.repo;

import com.kvs.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByTenantIdAndUsername(String tenantId, String username);

    Optional<User> findByTenantIdAndEmail(String tenantId, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
