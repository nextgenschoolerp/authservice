package com.kvs.auth.controller;

import com.kvs.auth.dto.SessionConfigRequest;
import com.kvs.auth.dto.SessionConfigResponse;
import com.kvs.auth.service.SessionConfigService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller managing session security configurations and idle timeouts.
 * Covers user story:
 * US-00004: Set global session idle timeout thresholds to 15 minutes
 * Actor: IT System Admin
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/security")
public class SessionConfigController {

    @Autowired
    private SessionConfigService sessionConfigService;

    @PutMapping("/session-config")
    @PreAuthorize("hasRole('IT_SYSTEM_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SessionConfigResponse>> configureSession(
            @Valid @RequestBody SessionConfigRequest request) {
        log.info("Request to update session configuration for tenantId: {}", request.getTenantId());
        SessionConfigResponse response = sessionConfigService.configureSession(request);
        return ResponseEntity.ok(ApiResponse.success("Session configuration updated successfully", response));
    }

    @GetMapping("/session-config")
    @PreAuthorize("hasRole('IT_SYSTEM_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SessionConfigResponse>> getSessionConfig(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to retrieve session configuration for tenantId: {}", tenantId);
        SessionConfigResponse response = sessionConfigService.getSessionConfig(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
