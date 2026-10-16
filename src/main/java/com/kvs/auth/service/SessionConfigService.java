package com.kvs.auth.service;

import com.kvs.auth.dto.SessionConfigRequest;
import com.kvs.auth.dto.SessionConfigResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.SessionConfig;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.SessionConfigRepository;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service managing session security configurations and idle timeout thresholds.
 * Covers user story:
 * US-00004: Set global session idle timeout thresholds to 15 minutes
 * Actor: IT System Admin
 */
@Service
@Slf4j
public class SessionConfigService {

    @Autowired
    private SessionConfigRepository sessionConfigRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public SessionConfigResponse configureSession(SessionConfigRequest request) {
        log.info("Configuring session security parameters for tenantId: {}", request.getTenantId());

        SessionConfig config = sessionConfigRepository.findByTenantId(request.getTenantId())
                .orElseGet(() -> {
                    SessionConfig newConfig = new SessionConfig();
                    newConfig.setTenantId(request.getTenantId());
                    return newConfig;
                });

        config.setIdleTimeoutMinutes(request.getIdleTimeoutMinutes());
        config.setMaxSessionDurationMinutes(request.getMaxSessionDurationMinutes());
        config.setConcurrentSessionsAllowed(request.isConcurrentSessionsAllowed());
        config.setMaxConcurrentSessions(request.getMaxConcurrentSessions() > 0 ? request.getMaxConcurrentSessions() : 1);

        SessionConfig saved = sessionConfigRepository.save(config);
        log.info("Saved session configuration for tenantId: {}, id: {}", request.getTenantId(), saved.getId());

        logAudit("SET_SESSION_IDLE_TIMEOUT", "tenant:" + request.getTenantId(), "SUCCESS",
                "Set idle timeout to " + request.getIdleTimeoutMinutes() + " mins, max duration: "
                        + request.getMaxSessionDurationMinutes() + " mins");

        return mapToResponse(saved);
    }

    public SessionConfigResponse getSessionConfig(String tenantId) {
        log.info("Retrieving session security parameters for tenantId: {}", tenantId);
        SessionConfig config = sessionConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionConfig", "tenantId", tenantId));
        return mapToResponse(config);
    }

    private void logAudit(String action, String resource, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("it_system_admin");
            String ipAddress = (httpRequest != null && httpRequest.getRemoteAddr() != null)
                    ? httpRequest.getRemoteAddr()
                    : "127.0.0.1";

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(currentUser);
            auditLog.setIpAddress(ipAddress);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setOutcome(outcome);
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    private SessionConfigResponse mapToResponse(SessionConfig entity) {
        return SessionConfigResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .idleTimeoutMinutes(entity.getIdleTimeoutMinutes())
                .maxSessionDurationMinutes(entity.getMaxSessionDurationMinutes())
                .concurrentSessionsAllowed(entity.isConcurrentSessionsAllowed())
                .maxConcurrentSessions(entity.getMaxConcurrentSessions())
                .build();
    }
}
