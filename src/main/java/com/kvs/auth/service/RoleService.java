package com.kvs.auth.service;

import com.kvs.auth.dto.*;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.Role;
import com.kvs.auth.entity.RolePermission;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.RolePermissionRepository;
import com.kvs.auth.repo.RoleRepository;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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

    private static final Pattern SCREEN_NAME = Pattern.compile("[A-Z][A-Z0-9_]{1,99}");

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
    public RoleResponse cloneRole(Long sourceRoleId, CloneRoleRequest request) {
        log.info("Cloning role from sourceRoleId: {} to new name: {}", sourceRoleId, request.getNewRoleName());
        Role sourceRole = roleRepository.findById(sourceRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", sourceRoleId));

        if (roleRepository.findByName(request.getNewRoleName()).isPresent()) {
            throw new ConflictException("Role with name " + request.getNewRoleName() + " already exists");
        }

        Role clonedRole = new Role();
        clonedRole.setName(request.getNewRoleName());
        clonedRole.setDescription(request.getNewRoleDescription() != null
                ? request.getNewRoleDescription()
                : "Cloned from " + sourceRole.getName() + ": " + sourceRole.getDescription());
        clonedRole.setActive(true);

        Role savedRole = roleRepository.save(clonedRole);

        List<RolePermission> sourcePermissions = rolePermissionRepository.findByRoleId(sourceRoleId);
        for (RolePermission srcPerm : sourcePermissions) {
            RolePermission newPerm = new RolePermission();
            newPerm.setRole(savedRole);
            newPerm.setScreenName(srcPerm.getScreenName());
            newPerm.setCanRead(srcPerm.isCanRead());
            newPerm.setCanWrite(srcPerm.isCanWrite());
            newPerm.setCanDelete(srcPerm.isCanDelete());
            newPerm.setCanExport(srcPerm.isCanExport());
            rolePermissionRepository.save(newPerm);
        }

        logAudit("CLONE_ROLE", "role:" + savedRole.getId(), "SUCCESS",
                "Cloned role from source " + sourceRole.getName() + " to " + savedRole.getName() +
                        " with " + sourcePermissions.size() + " permissions");

        return mapToRoleResponse(savedRole);
    }

    /**
     * Saves the listed screens of a role. A screen with every box unticked is removed from the role; screens not listed
     * are left as they are. Write, Delete and Export need Read. Returns the role's permissions after the change.
     */
    @Transactional
    public List<RolePermissionResponse> assignPermissions(Long roleId, List<RolePermissionRequest> requests) {
        log.info("Assigning screen permissions to roleId: {}", roleId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        Set<String> seen = new HashSet<>();
        for (RolePermissionRequest permReq : requests) {
            String screen = permReq.getScreenName().trim();
            if (!SCREEN_NAME.matcher(screen).matches()) {
                throw new BadRequestException("Screen name " + screen + " is not valid: use capital letters, digits and underscores, for example STUDENT_PROFILE");
            }
            if (!seen.add(screen)) {
                throw new BadRequestException("Screen " + screen + " is listed more than once");
            }
            if (!permReq.isCanRead() && (permReq.isCanWrite() || permReq.isCanDelete() || permReq.isCanExport())) {
                throw new BadRequestException("Screen " + screen + ": Write, Delete and Export need Read");
            }
            permReq.setScreenName(screen);
        }

        int saved = 0;
        int removed = 0;
        for (RolePermissionRequest permReq : requests) {
            Optional<RolePermission> existing = rolePermissionRepository.findByRoleIdAndScreenName(roleId, permReq.getScreenName());
            if (!permReq.isCanRead()) {
                if (existing.isPresent()) {
                    rolePermissionRepository.delete(existing.get());
                    removed++;
                }
                continue;
            }
            RolePermission perm = existing.orElseGet(() -> {
                RolePermission newPerm = new RolePermission();
                newPerm.setRole(role);
                newPerm.setScreenName(permReq.getScreenName());
                return newPerm;
            });
            perm.setCanRead(true);
            perm.setCanWrite(permReq.isCanWrite());
            perm.setCanDelete(permReq.isCanDelete());
            perm.setCanExport(permReq.isCanExport());
            rolePermissionRepository.save(perm);
            saved++;
        }
        rolePermissionRepository.flush();

        logAudit("ASSIGN_SCREEN_PERMISSIONS", "role:" + roleId, "SUCCESS",
                "Saved " + saved + " and removed " + removed + " screen permissions of role " + role.getName());

        return getPermissions(roleId);
    }

    public List<RolePermissionResponse> getPermissions(Long roleId) {
        log.info("Fetching permissions for roleId: {}", roleId);
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role", "id", roleId);
        }
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .sorted(Comparator.comparing(RolePermission::getScreenName))
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
