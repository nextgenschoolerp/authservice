package com.kvs.auth.controller;

import com.kvs.auth.dto.*;
import com.kvs.auth.service.BranchProvisioningService;
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
 * Controller managing newly onboarded branch database schema provisioning.
 * Covers user story:
 * US-00009: Provision isolated database schemas for newly onboarded school branches
 * Actor: Super Admin
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tenants")
public class BranchProvisioningController {

    @Autowired
    private BranchProvisioningService branchProvisioningService;

    @PostMapping("/{tenantId}/branches/provision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BranchProvisioningResponse>> provisionBranch(
            @PathVariable("tenantId") String tenantId,
            @Valid @RequestBody BranchProvisioningRequest request) {
        log.info("Request to provision branch for tenantId: {}, branchCode: {}", tenantId, request.getBranchCode());
        BranchProvisioningResponse response = branchProvisioningService.provisionBranch(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Isolated database schema provisioned successfully for branch", response));
    }

    @GetMapping("/{tenantId}/branches/{branchCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BranchProvisioningResponse>> getBranch(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("branchCode") String branchCode) {
        log.info("Request to fetch branch provisioning for tenantId: {}, branchCode: {}", tenantId, branchCode);
        BranchProvisioningResponse response = branchProvisioningService.getBranch(tenantId, branchCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{tenantId}/branches")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<BranchProvisioningResponse>>> listBranches(
            @PathVariable("tenantId") String tenantId) {
        log.info("Request to list branches for tenantId: {}", tenantId);
        List<BranchProvisioningResponse> response = branchProvisioningService.listBranches(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Monitor active user concurrency counts against plan seat limits (US-00018).
     *
     * @param tenantId   unique tenant identifier
     * @param branchCode branch identifier
     * @return 200 OK envelope with active concurrency metrics and seat utilization
     */
    @GetMapping("/{tenantId}/branches/{branchCode}/concurrency")
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasRole('PRINCIPAL') or hasRole('IT_SYSTEM_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.BranchConcurrencyResponse>> getBranchConcurrency(
            @PathVariable("tenantId") String tenantId,
            @PathVariable("branchCode") String branchCode) {
        log.info("Request to inspect active concurrency for tenantId: {}, branchCode: {}", tenantId, branchCode);
        com.kvs.auth.dto.BranchConcurrencyResponse response = branchProvisioningService.getBranchConcurrency(tenantId, branchCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
