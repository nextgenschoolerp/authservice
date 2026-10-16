package com.kvs.auth.controller;

import com.kvs.auth.dto.AnonymizeProfileRequest;
import com.kvs.auth.dto.PrivacyAnonymizationResponse;
import com.kvs.auth.service.PrivacyComplianceService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller managing GDPR and DPDP privacy anonymization workflows.
 * Covers user story:
 * US-00008: Anonymize inactive student and staff profiles to comply with privacy laws
 * Actor: Compliance Officer
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/compliance")
public class PrivacyComplianceController {

    @Autowired
    private PrivacyComplianceService complianceService;

    @PostMapping("/anonymize")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_COMPLIANCE_OFFICER') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PrivacyAnonymizationResponse>> anonymizeProfile(
            @Valid @RequestBody AnonymizeProfileRequest request) {
        log.info("Request to anonymize profile for userId: {} in tenant: {}", request.getUserId(), request.getTenantId());
        PrivacyAnonymizationResponse response = complianceService.anonymizeProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile anonymized successfully for GDPR/DPDP compliance", response));
    }

    @GetMapping("/anonymization-logs")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_COMPLIANCE_OFFICER') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PrivacyAnonymizationResponse>>> getAnonymizationLogs(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to fetch anonymization logs for tenant: {}", tenantId);
        List<PrivacyAnonymizationResponse> response = complianceService.getAnonymizationLogs(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
