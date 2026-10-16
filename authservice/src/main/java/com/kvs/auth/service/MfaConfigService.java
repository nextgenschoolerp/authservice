package com.kvs.auth.service;

import com.kvs.auth.dto.MfaConfigRequest;
import com.kvs.auth.dto.MfaConfigResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.MfaConfig;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.MfaConfigRepository;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service managing multi-factor authentication (MFA) enforcement policies.
 * Covers user story:
 * US-00003: Enforce mandatory totp/sms multi-factor authentication for sensitive transactions
 * Actor: Principal
 */
@Service
@Slf4j
public class MfaConfigService {

    @Autowired
    private MfaConfigRepository mfaConfigRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public MfaConfigResponse configureMfa(MfaConfigRequest request) {
        log.info("Configuring MFA policy for tenantId: {}", request.getTenantId());

        MfaConfig config = mfaConfigRepository.findByTenantId(request.getTenantId())
                .orElseGet(() -> {
                    MfaConfig newConfig = new MfaConfig();
                    newConfig.setTenantId(request.getTenantId());
                    return newConfig;
                });

        config.setTotpEnabled(request.isTotpEnabled());
        config.setSmsEnabled(request.isSmsEnabled());
        config.setMandatoryForSensitiveTransactions(request.isMandatoryForSensitiveTransactions());
        config.setMaxFailedAttempts(request.getMaxFailedAttempts() > 0 ? request.getMaxFailedAttempts() : 5);

        MfaConfig saved = mfaConfigRepository.save(config);
        log.info("Saved MFA configuration for tenantId: {}, id: {}", request.getTenantId(), saved.getId());

        logAudit("ENFORCE_MFA_POLICY", "tenant:" + request.getTenantId(), "SUCCESS",
                "Enforced MFA policy - TOTP: " + request.isTotpEnabled()
                        + ", SMS: " + request.isSmsEnabled()
                        + ", MandatoryForSensitive: " + request.isMandatoryForSensitiveTransactions());

        return mapToResponse(saved);
    }

    public MfaConfigResponse getMfaConfig(String tenantId) {
        log.info("Retrieving MFA policy for tenantId: {}", tenantId);
        MfaConfig config = mfaConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("MfaConfig", "tenantId", tenantId));
        return mapToResponse(config);
    }

    private void logAudit(String action, String resource, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("principal");
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

    private MfaConfigResponse mapToResponse(MfaConfig entity) {
        return MfaConfigResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .totpEnabled(entity.isTotpEnabled())
                .smsEnabled(entity.isSmsEnabled())
                .mandatoryForSensitiveTransactions(entity.isMandatoryForSensitiveTransactions())
                .maxFailedAttempts(entity.getMaxFailedAttempts())
                .build();
    }
}
