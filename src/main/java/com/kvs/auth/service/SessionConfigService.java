package com.kvs.auth.service;

import com.kvs.auth.dto.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

    @Transactional
    public IpWhitelistResponse configureIpWhitelist(IpWhitelistRequest request) {
        log.info("Configuring IP whitelist for tenantId: {}", request.getTenantId());
        SessionConfig config = sessionConfigRepository.findByTenantId(request.getTenantId())
                .orElseGet(() -> {
                    SessionConfig newConfig = new SessionConfig();
                    newConfig.setTenantId(request.getTenantId());
                    return newConfig;
                });

        config.setAllowedSubnets(String.join(",", request.getAllowedSubnets()));
        config.setEnforceCampusSubnet(request.isEnforceCampusSubnet());
        sessionConfigRepository.save(config);

        logAudit("CONFIGURE_IP_WHITELIST", "tenant:" + request.getTenantId(), "SUCCESS",
                "Configured " + request.getAllowedSubnets().size() + " subnets. Enforce: " + request.isEnforceCampusSubnet());

        return IpWhitelistResponse.builder()
                .tenantId(request.getTenantId())
                .allowedSubnets(request.getAllowedSubnets())
                .enforceCampusSubnet(request.isEnforceCampusSubnet())
                .status("ACTIVE")
                .build();
    }

    public IpWhitelistResponse getIpWhitelist(String tenantId) {
        log.info("Retrieving IP whitelist for tenantId: {}", tenantId);
        SessionConfig config = sessionConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionConfig", "tenantId", tenantId));

        List<String> subnets = config.getAllowedSubnets() != null && !config.getAllowedSubnets().isBlank()
                ? List.of(config.getAllowedSubnets().split(","))
                : List.of();

        return IpWhitelistResponse.builder()
                .tenantId(tenantId)
                .allowedSubnets(subnets)
                .enforceCampusSubnet(config.isEnforceCampusSubnet())
                .status(config.isEnforceCampusSubnet() ? "ENFORCED" : "PERMISSIVE")
                .build();
    }

    public IpValidationResponse validateIpAccess(IpValidationRequest request) {
        log.info("Validating access for IP: {} in tenant: {}", request.getIpAddress(), request.getTenantId());
        SessionConfig config = sessionConfigRepository.findByTenantId(request.getTenantId()).orElse(null);

        if (config == null || !config.isEnforceCampusSubnet() || config.getAllowedSubnets() == null || config.getAllowedSubnets().isBlank()) {
            return IpValidationResponse.builder()
                    .tenantId(request.getTenantId())
                    .ipAddress(request.getIpAddress())
                    .allowed(true)
                    .reason("Campus subnet enforcement not active")
                    .build();
        }

        String[] subnets = config.getAllowedSubnets().split(",");
        boolean matched = false;
        for (String subnet : subnets) {
            String trimmed = subnet.trim();
            if (trimmed.equals(request.getIpAddress()) || trimmed.equals("0.0.0.0/0") || request.getIpAddress().startsWith(trimmed.replace(".0/24", ""))) {
                matched = true;
                break;
            }
        }

        return IpValidationResponse.builder()
                .tenantId(request.getTenantId())
                .ipAddress(request.getIpAddress())
                .allowed(matched)
                .reason(matched ? "IP authorized by campus subnet policy" : "IP restricted outside authorized campus subnet")
                .build();
    }

    @Transactional
    public LockoutPolicyResponse configureLockoutPolicy(LockoutPolicyRequest request) {
        log.info("Configuring lockout policy for tenantId: {}", request.getTenantId());
        SessionConfig config = sessionConfigRepository.findByTenantId(request.getTenantId())
                .orElseGet(() -> {
                    SessionConfig newConfig = new SessionConfig();
                    newConfig.setTenantId(request.getTenantId());
                    return newConfig;
                });

        config.setMaxFailedAttempts(request.getMaxFailedAttempts());
        config.setLockoutDurationMinutes(request.getLockoutDurationMinutes());
        config.setAutoUnlockEnabled(request.isAutoUnlockEnabled());
        sessionConfigRepository.save(config);

        logAudit("CONFIGURE_LOCKOUT_POLICY", "tenant:" + request.getTenantId(), "SUCCESS",
                "Max attempts: " + request.getMaxFailedAttempts() + ", lockout duration: " + request.getLockoutDurationMinutes() + " mins");

        return LockoutPolicyResponse.builder()
                .tenantId(request.getTenantId())
                .maxFailedAttempts(request.getMaxFailedAttempts())
                .lockoutDurationMinutes(request.getLockoutDurationMinutes())
                .autoUnlockEnabled(request.isAutoUnlockEnabled())
                .status("CONFIGURED")
                .build();
    }

    public LockoutPolicyResponse getLockoutPolicy(String tenantId) {
        log.info("Retrieving lockout policy for tenantId: {}", tenantId);
        SessionConfig config = sessionConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionConfig", "tenantId", tenantId));

        return LockoutPolicyResponse.builder()
                .tenantId(tenantId)
                .maxFailedAttempts(config.getMaxFailedAttempts())
                .lockoutDurationMinutes(config.getLockoutDurationMinutes())
                .autoUnlockEnabled(config.isAutoUnlockEnabled())
                .status("ACTIVE")
                .build();
    }

    @Transactional
    public PasswordPolicyResponse configurePasswordPolicy(PasswordPolicyRequest request) {
        log.info("Configuring password policy for tenantId: {}", request.getTenantId());
        SessionConfig config = sessionConfigRepository.findByTenantId(request.getTenantId())
                .orElseGet(() -> {
                    SessionConfig newConfig = new SessionConfig();
                    newConfig.setTenantId(request.getTenantId());
                    return newConfig;
                });

        config.setMinPasswordLength(request.getMinLength());
        config.setRequireUppercase(request.isRequireUppercase());
        config.setRequireLowercase(request.isRequireLowercase());
        config.setRequireDigits(request.isRequireDigits());
        config.setRequireSpecialChars(request.isRequireSpecialChars());
        config.setPasswordExpiryDays(request.getExpiryDays());
        config.setPreventReuseCount(request.getPreventReuseCount());
        sessionConfigRepository.save(config);

        logAudit("CONFIGURE_PASSWORD_POLICY", "tenant:" + request.getTenantId(), "SUCCESS",
                "Min length: " + request.getMinLength() + ", special chars: " + request.isRequireSpecialChars());

        return PasswordPolicyResponse.builder()
                .tenantId(request.getTenantId())
                .minLength(request.getMinLength())
                .requireUppercase(request.isRequireUppercase())
                .requireLowercase(request.isRequireLowercase())
                .requireDigits(request.isRequireDigits())
                .requireSpecialChars(request.isRequireSpecialChars())
                .expiryDays(request.getExpiryDays())
                .preventReuseCount(request.getPreventReuseCount())
                .status("CONFIGURED")
                .build();
    }

    public PasswordPolicyResponse getPasswordPolicy(String tenantId) {
        log.info("Retrieving password policy for tenantId: {}", tenantId);
        SessionConfig config = sessionConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionConfig", "tenantId", tenantId));

        return PasswordPolicyResponse.builder()
                .tenantId(tenantId)
                .minLength(config.getMinPasswordLength())
                .requireUppercase(config.isRequireUppercase())
                .requireLowercase(config.isRequireLowercase())
                .requireDigits(config.isRequireDigits())
                .requireSpecialChars(config.isRequireSpecialChars())
                .expiryDays(config.getPasswordExpiryDays())
                .preventReuseCount(config.getPreventReuseCount())
                .status("ACTIVE")
                .build();
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
