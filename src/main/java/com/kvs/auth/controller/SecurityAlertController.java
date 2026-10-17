package com.kvs.auth.controller;

import com.kvs.auth.dto.SecurityAlertRuleRequest;
import com.kvs.auth.dto.SecurityAlertRuleResponse;
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
 * Controller managing automated security alert rules for anomalous login attempts and security events.
 * Covers user stories:
 * US-00020, US-00040, US-00060, US-00080, US-00100: Set up automated alert rules for anomalous login attempts from unknown locations
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/security/alert-rules")
public class SecurityAlertController {

    @Autowired
    private DisasterRecoveryService disasterRecoveryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ADMIN', 'SUPER_ADMIN', 'TRUSTEE', 'SECURITY_AUDITOR', 'COMPLIANCE_OFFICER') or hasAnyAuthority('ROLE_IT_SYSTEM_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_TRUSTEE', 'ROLE_SECURITY_AUDITOR', 'ROLE_COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<SecurityAlertRuleResponse>> createAlertRule(
            @Valid @RequestBody SecurityAlertRuleRequest request) {
        log.info("Request to create security alert rule '{}' for tenantId: {}", request.getRuleName(), request.getTenantId());
        SecurityAlertRuleResponse response = disasterRecoveryService.createAlertRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Automated security alert rule configured successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ADMIN', 'SUPER_ADMIN', 'TRUSTEE', 'SECURITY_AUDITOR', 'COMPLIANCE_OFFICER') or hasAnyAuthority('ROLE_IT_SYSTEM_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_TRUSTEE', 'ROLE_SECURITY_AUDITOR', 'ROLE_COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<SecurityAlertRuleResponse>>> getAlertRules(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to retrieve security alert rules for tenantId: {}", tenantId);
        List<SecurityAlertRuleResponse> response = disasterRecoveryService.getAlertRules(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Security alert rules retrieved successfully", response));
    }
}
