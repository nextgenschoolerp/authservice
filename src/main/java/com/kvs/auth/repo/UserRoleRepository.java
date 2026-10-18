package com.kvs.auth.repo;

import com.kvs.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserIdIn(Collection<String> userIds);

    List<UserRole> findByUserId(String userId);
}
