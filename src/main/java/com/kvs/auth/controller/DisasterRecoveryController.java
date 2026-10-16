package com.kvs.auth.controller;

import com.kvs.auth.dto.DisasterRecoveryBackupRequest;
import com.kvs.auth.dto.DisasterRecoveryBackupResponse;
import com.kvs.auth.service.DisasterRecoveryService;
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
 * Controller managing disaster recovery automated snapshot backups to encrypted cloud storage.
 * Covers user story:
 * US-00010: Execute automated database snapshot backups to encrypted cloud storage
 * Actor: Trustee
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/disaster-recovery/backups")
public class DisasterRecoveryController {

    @Autowired
    private DisasterRecoveryService disasterRecoveryService;

    @PostMapping
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DisasterRecoveryBackupResponse>> executeBackup(
            @Valid @RequestBody DisasterRecoveryBackupRequest request) {
        log.info("Request to execute database snapshot backup for tenantId: {}", request.getTenantId());
        DisasterRecoveryBackupResponse response = disasterRecoveryService.executeBackup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Database snapshot backup executed and stored successfully in encrypted cloud storage", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DisasterRecoveryBackupResponse>>> listBackups(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to list disaster recovery backups for tenantId: {}", tenantId);
        List<DisasterRecoveryBackupResponse> response = disasterRecoveryService.listBackups(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{backupId}")
    @PreAuthorize("hasRole('TRUSTEE') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DisasterRecoveryBackupResponse>> getBackup(
            @PathVariable("backupId") Long backupId) {
        log.info("Request to fetch backup details for id: {}", backupId);
        DisasterRecoveryBackupResponse response = disasterRecoveryService.getBackup(backupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
