package com.kvs.auth.repo;

import com.kvs.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    Optional<RolePermission> findByRoleIdAndScreenName(Long roleId, String screenName);
    void deleteByRoleId(Long roleId);
}
