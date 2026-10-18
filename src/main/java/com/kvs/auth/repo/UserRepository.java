package com.kvs.auth.repo;

import com.kvs.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByTenantIdOrderByUsernameAsc(String tenantId);

    Optional<User> findByUserId(String userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByTenantIdAndUsername(String tenantId, String username);

    Optional<User> findByTenantIdAndEmail(String tenantId, String email);

    boolean existsByUserId(String userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
