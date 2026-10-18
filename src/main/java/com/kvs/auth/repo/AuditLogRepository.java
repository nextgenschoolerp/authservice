package com.kvs.auth.repo;

import com.kvs.auth.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Optional filters are built as JPA Specifications that only add a predicate for the values actually supplied.
 * A JPQL "(:param IS NULL OR …)" binds untyped nulls, which PostgreSQL rejects with
 * "could not determine data type of parameter".
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /** Configuration changes surfaced by the high-privilege audit view (US-00015). */
    Set<String> HIGH_PRIVILEGE_ACTIONS = Set.of(
            "SUPER_ADMIN_REGISTER", "CLONE_ROLE", "ASSIGN_SCREEN_PERMISSIONS", "CONFIGURE_IP_WHITELIST",
            "CONFIGURE_LOCKOUT_POLICY", "USER_SESSIONS_REVOKED", "ADMIN_ACCOUNT_UNLOCKED", "ADMIN_PASSWORD_RESET",
            "SET_SESSION_IDLE_TIMEOUT", "FIELD_ENCRYPTION_ENFORCED", "SECURITY_ALERT_RULE_CONFIGURED", "SCHEMA_PROVISIONED");

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByIpAddress(String ipAddress);

    default List<AuditLog> findByFilters(String userId, String ipAddress, Instant from, Instant to) {
        return findAll(Specification.where(equal("userId", userId))
                .and(equal("ipAddress", ipAddress))
                .and(eventTimeBetween(from, to))
                .and(newestFirst()));
    }

    default List<AuditLog> findHighPrivilegeLogs(Instant from, Instant to) {
        Specification<AuditLog> highPrivilege = (root, query, cb) -> root.get("action").in(HIGH_PRIVILEGE_ACTIONS);
        return findAll(Specification.where(highPrivilege)
                .and(eventTimeBetween(from, to))
                .and(newestFirst()));
    }

    private static Specification<AuditLog> equal(String field, String value) {
        return (value == null || value.isBlank()) ? null : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private static Specification<AuditLog> eventTimeBetween(Instant from, Instant to) {
        return (root, query, cb) -> cb.and(
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("eventTime"), from),
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("eventTime"), to));
    }

    private static Specification<AuditLog> newestFirst() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("eventTime")));
            return null;
        };
    }
}
