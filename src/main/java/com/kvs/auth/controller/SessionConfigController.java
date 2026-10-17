package com.kvs.auth.controller;

import com.kvs.auth.dto.*;
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

    @PostMapping("/ip-whitelist")
    @PreAuthorize("hasRole('IT_SYSTEM_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRINCIPAL') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL')")
    public ResponseEntity<ApiResponse<IpWhitelistResponse>> configureIpWhitelist(
            @Valid @RequestBody com.kvs.auth.dto.IpWhitelistRequest request) {
        log.info("Request to update campus IP whitelist for tenantId: {}", request.getTenantId());
        com.kvs.auth.dto.IpWhitelistResponse response = sessionConfigService.configureIpWhitelist(request);
        return ResponseEntity.ok(ApiResponse.success("Campus IP whitelist updated successfully", response));
    }

    @GetMapping("/ip-whitelist")
    @PreAuthorize("hasRole('IT_SYSTEM_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRINCIPAL') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.IpWhitelistResponse>> getIpWhitelist(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to get campus IP whitelist for tenantId: {}", tenantId);
        com.kvs.auth.dto.IpWhitelistResponse response = sessionConfigService.getIpWhitelist(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/ip-whitelist/validate")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.IpValidationResponse>> validateIp(
            @Valid @RequestBody com.kvs.auth.dto.IpValidationRequest request) {
        log.info("Validating campus IP access for: {}", request.getIpAddress());
        com.kvs.auth.dto.IpValidationResponse response = sessionConfigService.validateIpAccess(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/lockout-policy")
    @PreAuthorize("hasRole('SECURITY_AUDITOR') or hasRole('IT_SYSTEM_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SECURITY_AUDITOR') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.LockoutPolicyResponse>> configureLockoutPolicy(
            @Valid @RequestBody com.kvs.auth.dto.LockoutPolicyRequest request) {
        log.info("Request to configure lockout policy for tenantId: {}", request.getTenantId());
        com.kvs.auth.dto.LockoutPolicyResponse response = sessionConfigService.configureLockoutPolicy(request);
        return ResponseEntity.ok(ApiResponse.success("Account lockout policy configured successfully", response));
    }

    @GetMapping("/lockout-policy")
    @PreAuthorize("hasRole('SECURITY_AUDITOR') or hasRole('IT_SYSTEM_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SECURITY_AUDITOR') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.LockoutPolicyResponse>> getLockoutPolicy(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to retrieve lockout policy for tenantId: {}", tenantId);
        com.kvs.auth.dto.LockoutPolicyResponse response = sessionConfigService.getLockoutPolicy(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/password-policy")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('IT_SYSTEM_ADMIN') or hasRole('TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_TRUSTEE')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.PasswordPolicyResponse>> configurePasswordPolicy(
            @Valid @RequestBody com.kvs.auth.dto.PasswordPolicyRequest request) {
        log.info("Request to configure password complexity policy for tenantId: {}", request.getTenantId());
        com.kvs.auth.dto.PasswordPolicyResponse response = sessionConfigService.configurePasswordPolicy(request);
        return ResponseEntity.ok(ApiResponse.success("Password complexity policy configured successfully", response));
    }

    @GetMapping("/password-policy")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('IT_SYSTEM_ADMIN') or hasRole('TRUSTEE') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_IT_SYSTEM_ADMIN') or hasAuthority('ROLE_TRUSTEE')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.PasswordPolicyResponse>> getPasswordPolicy(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to retrieve password complexity policy for tenantId: {}", tenantId);
        com.kvs.auth.dto.PasswordPolicyResponse response = sessionConfigService.getPasswordPolicy(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
