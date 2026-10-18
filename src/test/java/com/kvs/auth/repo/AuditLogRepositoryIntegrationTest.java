package com.kvs.auth.repo;

import com.kvs.auth.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the audit queries against the real database (PostgreSQL in application.yml), where untyped null
 * parameters used to fail with "could not determine data type of parameter". Rolled back after each test.
 * Covers: US-00005 (filtered audit query), US-00015 (high-privilege audit view)
 */
@SpringBootTest
@Transactional
class AuditLogRepositoryIntegrationTest {

    private static final String USER = "zztest.audit@kvs.com";
    private static final String IP = "10.99.99.1";

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @BeforeEach
    void setUp() {
        auditLogRepository.save(log("LOGIN_SUCCESS", now.minus(2, ChronoUnit.HOURS)));
        auditLogRepository.save(log("CLONE_ROLE", now.minus(1, ChronoUnit.HOURS)));
    }

    @Test
    @DisplayName("findByFilters with no filters returns all logs newest first")
    void findByFilters_noFilters() {
        List<AuditLog> logs = auditLogRepository.findByFilters(null, null, null, null);

        assertThat(logs).extracting(AuditLog::getUserId).contains(USER);
        assertThat(logs).isSortedAccordingTo((a, b) -> b.getEventTime().compareTo(a.getEventTime()));
    }

    @Test
    @DisplayName("findByFilters applies user, IP and time window; blank values are ignored")
    void findByFilters_withFilters() {
        assertThat(auditLogRepository.findByFilters(USER, IP, null, null))
                .extracting(AuditLog::getAction).containsExactly("CLONE_ROLE", "LOGIN_SUCCESS");
        assertThat(auditLogRepository.findByFilters(USER, "", now.minus(90, ChronoUnit.MINUTES), now))
                .extracting(AuditLog::getAction).containsExactly("CLONE_ROLE");
        assertThat(auditLogRepository.findByFilters(USER, null, null, now.minus(90, ChronoUnit.MINUTES)))
                .extracting(AuditLog::getAction).containsExactly("LOGIN_SUCCESS");
        assertThat(auditLogRepository.findByFilters("nobody@kvs.com", null, null, null)).isEmpty();
    }

    @Test
    @DisplayName("findHighPrivilegeLogs returns only configuration-change actions, with or without a window")
    void findHighPrivilegeLogs() {
        assertThat(auditLogRepository.findHighPrivilegeLogs(null, null))
                .extracting(AuditLog::getAction).allMatch(AuditLogRepository.HIGH_PRIVILEGE_ACTIONS::contains)
                .contains("CLONE_ROLE");
        assertThat(auditLogRepository.findHighPrivilegeLogs(now.minus(3, ChronoUnit.HOURS), now))
                .extracting(AuditLog::getUserId).contains(USER);
    }

    private AuditLog log(String action, Instant at) {
        AuditLog entry = new AuditLog();
        entry.setUserId(USER);
        entry.setIpAddress(IP);
        entry.setAction(action);
        entry.setResource("audit_test");
        entry.setOutcome("SUCCESS");
        entry.setEventTime(at);
        entry.setDetails("AuditLogRepositoryIntegrationTest");
        return entry;
    }
}
