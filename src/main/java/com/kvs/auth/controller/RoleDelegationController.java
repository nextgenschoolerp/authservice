package com.kvs.auth.controller;

import com.kvs.auth.dto.RoleDelegationRequest;
import com.kvs.auth.dto.RoleDelegationResponse;
import com.kvs.auth.service.RoleDelegationService;
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
 * Controller managing temporary role authority delegations during leave windows.
 * Covers user story:
 * US-00006: Delegate administrative authority temporarily during executive leave windows
 * Actor: Department Head
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/roles/delegations")
public class RoleDelegationController {

    @Autowired
    private RoleDelegationService roleDelegationService;

    @PostMapping
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_DEPARTMENT_HEAD') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDelegationResponse>> delegateAuthority(
            @Valid @RequestBody RoleDelegationRequest request) {
        log.info("Request to delegate role authority in tenant: {}", request.getTenantId());
        RoleDelegationResponse response = roleDelegationService.delegateAuthority(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role authority delegated successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_DEPARTMENT_HEAD') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDelegationResponse>>> getDelegations(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to fetch delegations for tenant: {}", tenantId);
        List<RoleDelegationResponse> response = roleDelegationService.getDelegations(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{delegationId}")
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_DEPARTMENT_HEAD') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> revokeDelegation(
            @PathVariable("delegationId") Long delegationId) {
        log.info("Request to revoke delegation id: {}", delegationId);
        roleDelegationService.revokeDelegation(delegationId);
        return ResponseEntity.ok(ApiResponse.success("Role delegation revoked successfully", null));
    }
}
