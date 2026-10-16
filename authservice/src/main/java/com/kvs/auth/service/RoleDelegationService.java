package com.kvs.auth.service;

import com.kvs.auth.dto.RoleDelegationRequest;
import com.kvs.auth.dto.RoleDelegationResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.RoleDelegation;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.RoleDelegationRepository;
import com.kvs.core.exception.BadRequestException;
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
 * Service managing temporary role authority delegations during leave windows.
 * Covers user story:
 * US-00006: Delegate administrative authority temporarily during executive leave windows
 * Actor: Department Head
 */
@Service
@Slf4j
public class RoleDelegationService {

    @Autowired
    private RoleDelegationRepository roleDelegationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public RoleDelegationResponse delegateAuthority(RoleDelegationRequest request) {
        log.info("Delegating role authority from {} to {} in tenant {}",
                request.getDelegatorUserId(), request.getDelegateeUserId(), request.getTenantId());

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Delegation end time must be strictly after start time");
        }

        RoleDelegation delegation = new RoleDelegation();
        delegation.setTenantId(request.getTenantId());
        delegation.setDelegatorUserId(request.getDelegatorUserId());
        delegation.setDelegateeUserId(request.getDelegateeUserId());
        delegation.setRoleId(request.getRoleId());
        delegation.setRoleName(request.getRoleName());
        delegation.setStartTime(request.getStartTime());
        delegation.setEndTime(request.getEndTime());
        delegation.setReason(request.getReason());
        delegation.setStatus("ACTIVE");

        RoleDelegation saved = roleDelegationRepository.save(delegation);
        log.info("Saved role delegation id: {}", saved.getId());

        logAudit("DELEGATE_AUTHORITY", "tenant:" + request.getTenantId(), "SUCCESS",
                "Delegated role " + request.getRoleName() + " from "
                        + request.getDelegatorUserId() + " to " + request.getDelegateeUserId()
                        + " until " + request.getEndTime());

        return mapToResponse(saved);
    }

    public List<RoleDelegationResponse> getDelegations(String tenantId) {
        log.info("Fetching delegations for tenantId: {}", tenantId);
        return roleDelegationRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeDelegation(Long delegationId) {
        log.info("Revoking role delegation id: {}", delegationId);
        RoleDelegation delegation = roleDelegationRepository.findById(delegationId)
                .orElseThrow(() -> new ResourceNotFoundException("RoleDelegation", "id", delegationId));

        delegation.setStatus("REVOKED");
        roleDelegationRepository.save(delegation);

        logAudit("REVOKE_DELEGATION", "delegation:" + delegationId, "SUCCESS",
                "Revoked role delegation id " + delegationId);
    }

    private void logAudit(String action, String resource, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("dept_head");
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

    private RoleDelegationResponse mapToResponse(RoleDelegation entity) {
        return RoleDelegationResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .delegatorUserId(entity.getDelegatorUserId())
                .delegateeUserId(entity.getDelegateeUserId())
                .roleId(entity.getRoleId())
                .roleName(entity.getRoleName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .build();
    }
}
