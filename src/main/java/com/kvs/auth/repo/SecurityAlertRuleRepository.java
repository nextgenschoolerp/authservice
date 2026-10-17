package com.kvs.auth.repo;

import com.kvs.auth.entity.SecurityAlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAlertRuleRepository extends JpaRepository<SecurityAlertRule, Long> {

    List<SecurityAlertRule> findByTenantId(String tenantId);
}
