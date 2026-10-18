package com.kvs.auth.repo;

import com.kvs.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    List<UserCredential> findByUserIdIn(Collection<String> userIds);

    Optional<UserCredential> findByUserId(String userId);
}
