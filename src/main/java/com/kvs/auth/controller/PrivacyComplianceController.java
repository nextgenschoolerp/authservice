package com.kvs.auth.controller;

import com.kvs.auth.dto.*;
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

    /**
     * Enforce field-level data encryption for stored student health records (US-00019).
     *
     * @param request entity name, encrypted fields, and algorithm
     * @return 200 OK envelope confirming encryption policy enforcement
     */
    @PostMapping("/field-encryption")
    @PreAuthorize("hasRole('PRINCIPAL') or hasRole('COMPLIANCE_OFFICER') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_COMPLIANCE_OFFICER') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.FieldEncryptionPolicyResponse>> configureFieldEncryption(
            @Valid @RequestBody com.kvs.auth.dto.FieldEncryptionPolicyRequest request) {
        log.info("Request to enforce field encryption on entity: {}", request.getEntityName());
        com.kvs.auth.dto.FieldEncryptionPolicyResponse response = complianceService.configureFieldEncryption(request);
        return ResponseEntity.ok(ApiResponse.success("Field-level data encryption policy enforced successfully", response));
    }

    /**
     * Retrieve active field encryption policies for a tenant (US-00019).
     *
     * @param tenantId unique tenant identifier
     * @return 200 OK envelope with list of encryption policies
     */
    @GetMapping("/field-encryption")
    @PreAuthorize("hasRole('PRINCIPAL') or hasRole('COMPLIANCE_OFFICER') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_COMPLIANCE_OFFICER') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<com.kvs.auth.dto.FieldEncryptionPolicyResponse>>> getFieldEncryptionPolicies(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to inspect field encryption policies for tenantId: {}", tenantId);
        List<com.kvs.auth.dto.FieldEncryptionPolicyResponse> response = complianceService.getFieldEncryptionPolicies(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
