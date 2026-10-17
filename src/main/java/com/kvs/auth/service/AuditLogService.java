package com.kvs.auth.service;

import com.kvs.auth.dto.AuditLogResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service providing real-time forensic query access to system audit logs.
 * Covers user story:
 * US-00005: Query real-time system audit logs filtered by ip address and user id
 * Actor: Security Auditor
 */
@Service
@Slf4j
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    public List<AuditLogResponse> queryAuditLogs(String userId, String ipAddress, Instant from, Instant to) {
        log.info("Querying audit logs - userId: {}, ipAddress: {}, from: {}, to: {}", userId, ipAddress, from, to);

        List<AuditLog> logs = auditLogRepository.findByFilters(userId, ipAddress, from, to);

        // Acceptance criteria: logs an audit entry when querying audit logs
        logAuditQuery(userId, ipAddress, logs.size());

        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AuditLogResponse> queryHighPrivilegeLogs(Instant from, Instant to) {
        log.info("Querying high-privilege configuration change audit logs - from: {}, to: {}", from, to);
        List<AuditLog> logs = auditLogRepository.findHighPrivilegeLogs(from, to);
        logAuditQuery("HIGH_PRIVILEGE", null, logs.size());
        return logs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void logAuditQuery(String filteredUserId, String filteredIp, int resultCount) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("security_auditor");
            String ipAddress = (httpRequest != null && httpRequest.getRemoteAddr() != null)
                    ? httpRequest.getRemoteAddr()
                    : "127.0.0.1";

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(currentUser);
            auditLog.setIpAddress(ipAddress);
            auditLog.setAction("QUERY_AUDIT_LOGS");
            auditLog.setResource("audit_logs");
            auditLog.setOutcome("SUCCESS");
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails("Queried audit logs with filters [userId=" + filteredUserId
                    + ", ip=" + filteredIp + "]. Found " + resultCount + " records.");

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log for audit query: {}", e.getMessage());
        }
    }

    private AuditLogResponse mapToResponse(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ipAddress(entity.getIpAddress())
                .action(entity.getAction())
                .resource(entity.getResource())
                .outcome(entity.getOutcome())
                .eventTime(entity.getEventTime())
                .details(entity.getDetails())
                .build();
    }
}
