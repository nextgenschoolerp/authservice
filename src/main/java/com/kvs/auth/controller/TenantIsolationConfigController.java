package com.kvs.auth.controller;

import com.kvs.auth.dto.*;
import com.kvs.auth.service.TenantIsolationConfigService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller managing multi-tenant database isolation parameters.
 *
 * Covers user stories:
 * US-00001: Configure multi-tenant database isolation parameters - Scenario 1
 * Actors: Super Admin
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tenants")
public class TenantIsolationConfigController {

    @Autowired
    private TenantIsolationConfigService tenantIsolationConfigService;

    /**
     * Configure multi-tenant database isolation parameters.
     *
     * @param tenantId the unique tenant branch identifier
     * @param request  configuration details including schema name and security switches
     * @return response envelope containing updated isolation config
     */
    @PutMapping("/{tenantId}/isolation-config")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantIsolationConfigResponse>> configureIsolation(
            @PathVariable("tenantId") String tenantId,
            @Valid @RequestBody TenantIsolationConfigRequest request) {
        log.info("Request to configure tenant isolation parameters for tenantId: {}", tenantId);
        TenantIsolationConfigResponse response = tenantIsolationConfigService.configureIsolation(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Tenant database isolation configured successfully", response));
    }

    /**
     * Retrieve multi-tenant database isolation parameters for a tenant.
     *
     * @param tenantId the unique tenant branch identifier
     * @return response envelope containing current isolation config
     */
    @GetMapping("/{tenantId}/isolation-config")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantIsolationConfigResponse>> getIsolationConfig(
            @PathVariable("tenantId") String tenantId) {
        log.info("Request to retrieve tenant isolation parameters for tenantId: {}", tenantId);
        TenantIsolationConfigResponse response = tenantIsolationConfigService.getIsolationConfig(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Manage SSL/TLS certificate renewals and secure HTTPS domain routing (US-00016).
     *
     * @param tenantId unique tenant identifier
     * @param request  domain name, TLS version, and renewal flags
     * @return 200 OK envelope with updated certificate status
     */
    @PostMapping("/{tenantId}/ssl-certificate")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('SUPER_ADMIN') or hasRole('IT_SYSTEM_ADMIN') or hasAuthority('ROLE_COMPLIANCE_OFFICER') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.SslCertificateResponse>> configureSslCertificate(
            @PathVariable("tenantId") String tenantId,
            @Valid @RequestBody com.kvs.auth.dto.SslCertificateRequest request) {
        log.info("Request to configure SSL certificate for tenantId: {}", tenantId);
        com.kvs.auth.dto.SslCertificateResponse response = tenantIsolationConfigService.configureSslCertificate(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("SSL/TLS certificate and HTTPS routing configured successfully", response));
    }

    /**
     * Retrieve SSL/TLS certificate renewal status (US-00016).
     *
     * @param tenantId unique tenant identifier
     * @return 200 OK envelope with certificate metadata
     */
    @GetMapping("/{tenantId}/ssl-certificate")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('SUPER_ADMIN') or hasRole('IT_SYSTEM_ADMIN') or hasAuthority('ROLE_COMPLIANCE_OFFICER') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.SslCertificateResponse>> getSslCertificate(
            @PathVariable("tenantId") String tenantId) {
        log.info("Request to get SSL certificate details for tenantId: {}", tenantId);
        com.kvs.auth.dto.SslCertificateResponse response = tenantIsolationConfigService.getSslCertificate(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
