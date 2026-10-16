package com.kvs.auth.controller;

import com.kvs.auth.dto.MfaConfigRequest;
import com.kvs.auth.dto.MfaConfigResponse;
import com.kvs.auth.service.MfaConfigService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller managing multi-factor authentication policies.
 * Covers user story:
 * US-00003: Enforce mandatory totp/sms multi-factor authentication for sensitive transactions
 * Actor: Principal
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/mfa")
public class MfaConfigController {

    @Autowired
    private MfaConfigService mfaConfigService;

    @PutMapping("/config")
    @PreAuthorize("hasRole('PRINCIPAL') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MfaConfigResponse>> configureMfa(
            @Valid @RequestBody MfaConfigRequest request) {
        log.info("Request to configure MFA policy for tenantId: {}", request.getTenantId());
        MfaConfigResponse response = mfaConfigService.configureMfa(request);
        return ResponseEntity.ok(ApiResponse.success("MFA policy configured successfully", response));
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('PRINCIPAL') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MfaConfigResponse>> getMfaConfig(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to retrieve MFA policy for tenantId: {}", tenantId);
        MfaConfigResponse response = mfaConfigService.getMfaConfig(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
