package com.kvs.auth.service;

import com.kvs.auth.dto.*;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.TenantIsolationConfig;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.TenantIsolationConfigRepository;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service managing multi-tenant database isolation parameters and audit logging.
 *
 * Covers user story:
 * US-00001: Configure multi-tenant database isolation parameters - Scenario 1
 * Actor: Super Admin
 */
@Service
@Slf4j
public class TenantIsolationConfigService {

    @Autowired
    private TenantIsolationConfigRepository tenantIsolationConfigRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    /**
     * Configure or update multi-tenant database isolation parameters for a tenant.
     * Validates transaction, updates database state, and records an audit log entry.
     *
     * @param tenantId the unique tenant identifier
     * @param request  configuration parameters
     * @return updated tenant isolation configuration response
     */
    @Transactional
    public TenantIsolationConfigResponse configureIsolation(String tenantId, TenantIsolationConfigRequest request) {
        log.info("Configuring multi-tenant database isolation parameters for tenantId: {}", tenantId);

        TenantIsolationConfig config = tenantIsolationConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantIsolationConfig newConfig = new TenantIsolationConfig();
                    newConfig.setTenantId(tenantId);
                    return newConfig;
                });

        config.setSchemaName(request.getSchemaName());
        config.setCrossTenantAccessBlocked(request.isCrossTenantAccessBlocked());
        config.setEncryptionAlgorithm(request.getEncryptionAlgorithm());
        config.setRowLevelSecurityEnabled(request.isRowLevelSecurityEnabled());

        TenantIsolationConfig saved = tenantIsolationConfigRepository.save(config);
        log.info("Saved tenant isolation config for tenantId: {}, id: {}", tenantId, saved.getId());

        // Acceptance criteria: logs an audit entry confirming tenant isolation configuration
        logAudit(tenantId, "CONFIGURE_TENANT_ISOLATION", "SUCCESS",
                "Configured multi-tenant database isolation parameters for tenant " + tenantId
                        + " with schema: " + request.getSchemaName()
                        + ", crossTenantBlocked: " + request.isCrossTenantAccessBlocked());

        return mapToResponse(saved);
    }

    /**
     * Retrieve multi-tenant database isolation parameters by tenant identifier.
     *
     * @param tenantId the unique tenant identifier
     * @return current tenant isolation configuration
     * @throws ResourceNotFoundException if configuration does not exist for tenantId
     */
    public TenantIsolationConfigResponse getIsolationConfig(String tenantId) {
        log.info("Retrieving tenant isolation parameters for tenantId: {}", tenantId);
        TenantIsolationConfig config = tenantIsolationConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantIsolationConfig", "tenantId", tenantId));
        return mapToResponse(config);
    }

    @Transactional
    public com.kvs.auth.dto.SslCertificateResponse configureSslCertificate(String tenantId, com.kvs.auth.dto.SslCertificateRequest request) {
        log.info("Configuring SSL/TLS certificate for tenantId: {}, domain: {}", tenantId, request.getDomainName());
        TenantIsolationConfig config = tenantIsolationConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantIsolationConfig newConfig = new TenantIsolationConfig();
                    newConfig.setTenantId(tenantId);
                    newConfig.setSchemaName("tenant_" + tenantId.replace("-", "_").toLowerCase());
                    return newConfig;
                });

        config.setDomainName(request.getDomainName());
        config.setSslEnabled(request.isSslEnabled());
        config.setSslAutoRenewEnabled(request.isAutoRenewEnabled());
        config.setTlsVersion(request.getTlsVersion() != null ? request.getTlsVersion() : "TLSv1.3");
        config.setSslExpiresAt(Instant.now().plus(90, java.time.temporal.ChronoUnit.DAYS));

        TenantIsolationConfig saved = tenantIsolationConfigRepository.save(config);

        logAudit(tenantId, "CONFIGURE_SSL_CERTIFICATE", "SUCCESS",
                "Configured SSL/TLS domain routing for: " + request.getDomainName() + ", TLS: " + config.getTlsVersion());

        return com.kvs.auth.dto.SslCertificateResponse.builder()
                .tenantId(tenantId)
                .domainName(saved.getDomainName())
                .sslEnabled(saved.isSslEnabled())
                .autoRenewEnabled(saved.isSslAutoRenewEnabled())
                .tlsVersion(saved.getTlsVersion())
                .certificateExpiresAt(saved.getSslExpiresAt())
                .status("ACTIVE_SECURED")
                .build();
    }

    public com.kvs.auth.dto.SslCertificateResponse getSslCertificate(String tenantId) {
        log.info("Retrieving SSL certificate details for tenantId: {}", tenantId);
        TenantIsolationConfig config = tenantIsolationConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantIsolationConfig", "tenantId", tenantId));

        return com.kvs.auth.dto.SslCertificateResponse.builder()
                .tenantId(tenantId)
                .domainName(config.getDomainName() != null ? config.getDomainName() : tenantId + ".schoolerp.internal")
                .sslEnabled(config.isSslEnabled())
                .autoRenewEnabled(config.isSslAutoRenewEnabled())
                .tlsVersion(config.getTlsVersion() != null ? config.getTlsVersion() : "TLSv1.3")
                .certificateExpiresAt(config.getSslExpiresAt() != null ? config.getSslExpiresAt() : Instant.now().plus(90, java.time.temporal.ChronoUnit.DAYS))
                .status(config.isSslEnabled() ? "ACTIVE_SECURED" : "EXPIRED_OR_DISABLED")
                .build();
    }

    private void logAudit(String tenantId, String action, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("super_admin");
            String ipAddress = (httpRequest != null && httpRequest.getRemoteAddr() != null)
                    ? httpRequest.getRemoteAddr()
                    : "127.0.0.1";

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(currentUser);
            auditLog.setIpAddress(ipAddress);
            auditLog.setAction(action);
            auditLog.setResource("tenant:" + tenantId);
            auditLog.setOutcome(outcome);
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);

            auditLogRepository.save(auditLog);
            log.debug("Recorded audit entry for tenantId: {}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to record audit log for tenantId {}: {}", tenantId, e.getMessage());
        }
    }

    private TenantIsolationConfigResponse mapToResponse(TenantIsolationConfig entity) {
        return TenantIsolationConfigResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .schemaName(entity.getSchemaName())
                .crossTenantAccessBlocked(entity.isCrossTenantAccessBlocked())
                .encryptionAlgorithm(entity.getEncryptionAlgorithm())
                .rowLevelSecurityEnabled(entity.isRowLevelSecurityEnabled())
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt())
                .build();
    }
}
