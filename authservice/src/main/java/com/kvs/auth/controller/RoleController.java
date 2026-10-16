package com.kvs.auth.controller;

import com.kvs.auth.dto.*;
import com.kvs.auth.service.RoleService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller managing custom roles and screen-level permission assignments.
 * Covers user story:
 * US-00002: Assign screen-level read/write/delete/export permissions to custom roles
 * Actor: Trustee
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        log.info("Request to create custom role: {}", request.getName());
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", response));
    }

    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> assignPermissions(
            @PathVariable("roleId") Long roleId,
            @Valid @RequestBody AssignScreenPermissionsRequest request) {
        log.info("Request to assign screen permissions to roleId: {}", roleId);
        List<RolePermissionResponse> response = roleService.assignPermissions(roleId, request.getPermissions());
        return ResponseEntity.ok(ApiResponse.success("Screen permissions assigned successfully", response));
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> getPermissions(
            @PathVariable("roleId") Long roleId) {
        log.info("Request to fetch screen permissions for roleId: {}", roleId);
        List<RolePermissionResponse> response = roleService.getPermissions(roleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> response = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
