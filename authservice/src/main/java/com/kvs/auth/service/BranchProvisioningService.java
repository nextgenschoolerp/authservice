package com.kvs.auth.service;

import com.kvs.auth.dto.BranchProvisioningRequest;
import com.kvs.auth.dto.BranchProvisioningResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.BranchProvisioning;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.BranchProvisioningRepository;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
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
 * Service managing newly onboarded school branch isolated database schema provisioning.
 * Covers user story:
 * US-00009: Provision isolated database schemas for newly onboarded school branches
 * Actor: Super Admin
 */
@Service
@Slf4j
public class BranchProvisioningService {

    @Autowired
    private BranchProvisioningRepository branchProvisioningRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public BranchProvisioningResponse provisionBranch(String tenantId, BranchProvisioningRequest request) {
        log.info("Provisioning isolated database schema for tenantId: {}, branchCode: {}, schema: {}",
                tenantId, request.getBranchCode(), request.getSchemaName());

        branchProvisioningRepository.findByTenantIdAndBranchCode(tenantId, request.getBranchCode())
                .ifPresent(b -> {
                    throw new ConflictException("Branch with code " + request.getBranchCode()
                            + " already exists for tenant " + tenantId);
                });

        branchProvisioningRepository.findBySchemaName(request.getSchemaName())
                .ifPresent(b -> {
                    throw new ConflictException("Schema " + request.getSchemaName() + " is already in use");
                });

        BranchProvisioning branch = new BranchProvisioning();
        branch.setTenantId(tenantId);
        branch.setBranchCode(request.getBranchCode());
        branch.setBranchName(request.getBranchName());
        branch.setAffiliationBoard(request.getAffiliationBoard());
        branch.setSchemaName(request.getSchemaName());
        branch.setStudentCapacity(request.getStudentCapacity());
        branch.setStaffCapacity(request.getStaffCapacity());
        branch.setIsolatedDbEnabled(request.isIsolatedDbEnabled());
        branch.setStatus("PROVISIONED");

        BranchProvisioning saved = branchProvisioningRepository.save(branch);
        log.info("Successfully provisioned branch schema id: {}", saved.getId());

        logAudit(tenantId, "PROVISION_BRANCH_SCHEMA", "SUCCESS",
                "Provisioned isolated database schema " + request.getSchemaName()
                        + " for branch " + request.getBranchCode() + " (" + request.getBranchName() + ")"
                        + " with student capacity " + request.getStudentCapacity());

        return mapToResponse(saved);
    }

    public BranchProvisioningResponse getBranch(String tenantId, String branchCode) {
        log.info("Fetching branch provisioning details for tenantId: {}, branchCode: {}", tenantId, branchCode);
        BranchProvisioning branch = branchProvisioningRepository.findByTenantIdAndBranchCode(tenantId, branchCode)
                .orElseThrow(() -> new ResourceNotFoundException("BranchProvisioning", "branchCode", branchCode));
        return mapToResponse(branch);
    }

    public List<BranchProvisioningResponse> listBranches(String tenantId) {
        log.info("Fetching branches for tenantId: {}", tenantId);
        return branchProvisioningRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
            auditLog.setResource("branch:" + tenantId);
            auditLog.setOutcome(outcome);
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    private BranchProvisioningResponse mapToResponse(BranchProvisioning entity) {
        return BranchProvisioningResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .affiliationBoard(entity.getAffiliationBoard())
                .schemaName(entity.getSchemaName())
                .studentCapacity(entity.getStudentCapacity())
                .staffCapacity(entity.getStaffCapacity())
                .isolatedDbEnabled(entity.isIsolatedDbEnabled())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
