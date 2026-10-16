package com.kvs.auth.service;

import com.kvs.auth.dto.*;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.Role;
import com.kvs.auth.entity.RolePermission;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.RolePermissionRepository;
import com.kvs.auth.repo.RoleRepository;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing RBAC custom roles and screen-level permission assignments.
 * Covers user story:
 * US-00002: Assign screen-level read/write/delete/export permissions to custom roles
 * Actor: Trustee
 */
@Service
@Slf4j
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        log.info("Creating role: {}", request.getName());
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("Role with name " + request.getName() + " already exists");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setActive(true);

        Role saved = roleRepository.save(role);
        logAudit("CREATE_ROLE", "role:" + saved.getId(), "SUCCESS",
                "Created role: " + saved.getName());

        return mapToRoleResponse(saved);
    }

    @Transactional
    public List<RolePermissionResponse> assignPermissions(Long roleId, List<RolePermissionRequest> requests) {
        log.info("Assigning screen permissions to roleId: {}", roleId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        List<RolePermissionResponse> responses = new ArrayList<>();

        for (RolePermissionRequest permReq : requests) {
            RolePermission perm = rolePermissionRepository.findByRoleIdAndScreenName(roleId, permReq.getScreenName())
                    .orElseGet(() -> {
                        RolePermission newPerm = new RolePermission();
                        newPerm.setRole(role);
                        newPerm.setScreenName(permReq.getScreenName());
                        return newPerm;
                    });

            perm.setCanRead(permReq.isCanRead());
            perm.setCanWrite(permReq.isCanWrite());
            perm.setCanDelete(permReq.isCanDelete());
            perm.setCanExport(permReq.isCanExport());

            RolePermission saved = rolePermissionRepository.save(perm);
            responses.add(mapToPermissionResponse(saved));
        }

        logAudit("ASSIGN_SCREEN_PERMISSIONS", "role:" + roleId, "SUCCESS",
                "Assigned " + requests.size() + " screen permissions to role " + role.getName());

        return responses;
    }

    public List<RolePermissionResponse> getPermissions(Long roleId) {
        log.info("Fetching permissions for roleId: {}", roleId);
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role", "id", roleId);
        }
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        return mapToRoleResponse(role);
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToRoleResponse)
                .collect(Collectors.toList());
    }

    private void logAudit(String action, String resource, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("trustee");
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

    private RoleResponse mapToRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.isActive())
                .build();
    }

    private RolePermissionResponse mapToPermissionResponse(RolePermission perm) {
        return RolePermissionResponse.builder()
                .id(perm.getId())
                .screenName(perm.getScreenName())
                .canRead(perm.isCanRead())
                .canWrite(perm.isCanWrite())
                .canDelete(perm.isCanDelete())
                .canExport(perm.isCanExport())
                .build();
    }
}
