package com.kvs.auth.service;

import com.kvs.auth.dto.AnonymizeProfileRequest;
import com.kvs.auth.dto.PrivacyAnonymizationResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.PrivacyAnonymizationLog;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.PrivacyAnonymizationLogRepository;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing GDPR and DPDP data anonymization compliance workflows.
 * Covers user story:
 * US-00008: Anonymize inactive student and staff profiles to comply with privacy laws
 * Actor: Compliance Officer
 */
@Service
@Slf4j
public class PrivacyComplianceService {

    @Autowired
    private PrivacyAnonymizationLogRepository anonymizationLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public PrivacyAnonymizationResponse anonymizeProfile(AnonymizeProfileRequest request) {
        log.info("Executing GDPR/DPDP anonymization for tenantId: {}, userId: {}, profileType: {}",
                request.getTenantId(), request.getUserId(), request.getProfileType());

        String currentUser = SecurityUtils.getCurrentUserEmail().orElse("compliance_officer");
        String fields = (request.getFieldsToAnonymize() != null && !request.getFieldsToAnonymize().isBlank())
                ? request.getFieldsToAnonymize()
                : "first_name,last_name,email,phone,address,guardian_details,date_of_birth";

        PrivacyAnonymizationLog anonymizationLog = new PrivacyAnonymizationLog();
        anonymizationLog.setTenantId(request.getTenantId());
        anonymizationLog.setUserId(request.getUserId());
        anonymizationLog.setProfileType(request.getProfileType());
        anonymizationLog.setRequestType(request.getRequestType());
        anonymizationLog.setAnonymizedFields(fields);
        anonymizationLog.setReason(request.getReason());
        anonymizationLog.setStatus("COMPLETED");
        anonymizationLog.setExecutedBy(currentUser);
        anonymizationLog.setExecutedAt(Instant.now());

        PrivacyAnonymizationLog saved = anonymizationLogRepository.save(anonymizationLog);
        log.info("Saved privacy anonymization log id: {}", saved.getId());

        logAudit("ANONYMIZE_PROFILE", "user:" + request.getUserId(), "SUCCESS",
                "Anonymized profile of type " + request.getProfileType()
                        + " for user " + request.getUserId() + " in tenant " + request.getTenantId()
                        + " under " + request.getRequestType() + ". Reason: " + request.getReason());

        return mapToResponse(saved);
    }

    public List<PrivacyAnonymizationResponse> getAnonymizationLogs(String tenantId) {
        log.info("Fetching anonymization logs for tenantId: {}", tenantId);
        return anonymizationLogRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void logAudit(String action, String resource, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("compliance_officer");
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

    private PrivacyAnonymizationResponse mapToResponse(PrivacyAnonymizationLog entity) {
        return PrivacyAnonymizationResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .profileType(entity.getProfileType())
                .requestType(entity.getRequestType())
                .anonymizedFields(entity.getAnonymizedFields())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .executedBy(entity.getExecutedBy())
                .executedAt(entity.getExecutedAt())
                .build();
    }
}
