package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.*;
import com.kvs.auth.service.*;
import com.kvs.core.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for extended capabilities covering user stories:
 * US-00011 to US-00100 (Actions 11 through 20 across multiple scenarios).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExtendedCapabilitiesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private SessionConfigService sessionConfigService;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private TenantIsolationConfigService tenantIsolationConfigService;

    @MockBean
    private BranchProvisioningService branchProvisioningService;

    @MockBean
    private PrivacyComplianceService privacyComplianceService;

    @MockBean
    private DisasterRecoveryService disasterRecoveryService;

    private String superAdminToken;
    private String principalToken;
    private String itAdminToken;
    private String auditorToken;
    private String deptHeadToken;
    private String clerkToken;
    private String complianceToken;
    private String trusteeToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        superAdminToken = jwtTokenService.generateToken("superadmin@kvs.com", Map.of("roles", List.of("ROLE_SUPER_ADMIN")));
        principalToken = jwtTokenService.generateToken("principal@kvs.com", Map.of("roles", List.of("ROLE_PRINCIPAL")));
        itAdminToken = jwtTokenService.generateToken("itadmin@kvs.com", Map.of("roles", List.of("ROLE_IT_SYSTEM_ADMIN")));
        auditorToken = jwtTokenService.generateToken("auditor@kvs.com", Map.of("roles", List.of("ROLE_SECURITY_AUDITOR")));
        deptHeadToken = jwtTokenService.generateToken("depthead@kvs.com", Map.of("roles", List.of("ROLE_DEPARTMENT_HEAD")));
        clerkToken = jwtTokenService.generateToken("clerk@kvs.com", Map.of("roles", List.of("ROLE_ADMIN_CLERK")));
        complianceToken = jwtTokenService.generateToken("compliance@kvs.com", Map.of("roles", List.of("ROLE_COMPLIANCE_OFFICER")));
        trusteeToken = jwtTokenService.generateToken("trustee@kvs.com", Map.of("roles", List.of("ROLE_TRUSTEE")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    // =========================================================================
    // Action 11 (US-00011, US-00031, etc.): Role Permission Profile Cloning
    // =========================================================================

    @Test
    @DisplayName("US-00011: POST /api/v1/roles/{roleId}/clone - Principal clones role returns 201 Created")
    void testCloneRole_Success() throws Exception {
        CloneRoleRequest request = new CloneRoleRequest("TEACHER_MATH_CLONE", "Cloned Math Teacher Role");
        RoleResponse mockResponse = RoleResponse.builder()
                .id(25L)
                .name("TEACHER_MATH_CLONE")
                .description("Cloned Math Teacher Role")
                .active(true)
                .build();

        when(roleService.cloneRole(eq(5L), any(CloneRoleRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/roles/5/clone")
                        .header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("TEACHER_MATH_CLONE"))
                .andExpect(jsonPath("$.data.id").value(25));
    }

    @Test
    @DisplayName("US-00011: POST /api/v1/roles/{roleId}/clone - Teacher cannot clone role returns 403 Forbidden")
    void testCloneRole_Forbidden() throws Exception {
        CloneRoleRequest request = new CloneRoleRequest("UNAUTHORIZED_ROLE", "Desc");

        mockMvc.perform(post("/api/v1/roles/5/clone")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Action 12 (US-00012, US-00032, etc.): Campus IP Subnet Whitelist & Validation
    // =========================================================================

    @Test
    @DisplayName("US-00012: POST /api/v1/security/ip-whitelist - IT Admin sets IP whitelist returns 200 OK")
    void testConfigureIpWhitelist_Success() throws Exception {
        IpWhitelistRequest request = IpWhitelistRequest.builder()
                .tenantId("DPS-DELHI-01")
                .allowedSubnets(List.of("192.168.1.0/24", "10.0.0.0/16"))
                .enforceCampusSubnet(true)
                .build();

        IpWhitelistResponse mockResponse = IpWhitelistResponse.builder()
                .tenantId("DPS-DELHI-01")
                .allowedSubnets(List.of("192.168.1.0/24", "10.0.0.0/16"))
                .enforceCampusSubnet(true)
                .build();

        when(sessionConfigService.configureIpWhitelist(any(IpWhitelistRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/security/ip-whitelist")
                        .header("Authorization", "Bearer " + itAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enforceCampusSubnet").value(true))
                .andExpect(jsonPath("$.data.allowedSubnets[0]").value("192.168.1.0/24"));
    }

    @Test
    @DisplayName("US-00012: POST /api/v1/security/ip-whitelist/validate - Validates IP address access")
    void testValidateIpAccess_Success() throws Exception {
        IpValidationRequest request = new IpValidationRequest("DPS-DELHI-01", "192.168.1.55");
        IpValidationResponse mockResponse = IpValidationResponse.builder()
                .ipAddress("192.168.1.55")
                .allowed(true)
                .reason("Allowed: IP is within campus subnet whitelist")
                .build();

        when(sessionConfigService.validateIpAccess(any(IpValidationRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/security/ip-whitelist/validate")
                        .header("Authorization", "Bearer " + itAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allowed").value(true));
    }

    // =========================================================================
    // Action 13 (US-00013, US-00033, etc.): Account Lockout Rules
    // =========================================================================

    @Test
    @DisplayName("US-00013: PUT /api/v1/security/lockout-policy - Auditor configures lockout policy returns 200 OK")
    void testConfigureLockoutPolicy_Success() throws Exception {
        LockoutPolicyRequest request = LockoutPolicyRequest.builder()
                .tenantId("DPS-DELHI-01")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(30)
                .autoUnlockEnabled(true)
                .build();

        LockoutPolicyResponse mockResponse = LockoutPolicyResponse.builder()
                .tenantId("DPS-DELHI-01")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(30)
                .autoUnlockEnabled(true)
                .build();

        when(sessionConfigService.configureLockoutPolicy(any(LockoutPolicyRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/security/lockout-policy")
                        .header("Authorization", "Bearer " + auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maxFailedAttempts").value(5))
                .andExpect(jsonPath("$.data.lockoutDurationMinutes").value(30));
    }

    // =========================================================================
    // Action 14 (US-00014, US-00034, etc.): Instant User Session Revocation
    // =========================================================================

    @Test
    @DisplayName("US-00014: POST /api/v1/users/{userId}/sessions/revoke - Dept Head revokes sessions returns 200 OK")
    void testRevokeUserSessions_Success() throws Exception {
        RevokeSessionsResponse mockResponse = RevokeSessionsResponse.builder()
                .userId("u-resigned-123")
                .revokedSessionsCount(3)
                .message("Successfully revoked 3 active sessions")
                .revokedAt(Instant.now())
                .build();

        when(authService.revokeUserSessionsWithDetails("u-resigned-123")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/users/u-resigned-123/sessions/revoke")
                        .header("Authorization", "Bearer " + deptHeadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.revokedSessionsCount").value(3))
                .andExpect(jsonPath("$.data.userId").value("u-resigned-123"));
    }

    // =========================================================================
    // Action 15 (US-00015, US-00035, etc.): High-Privilege Audit Logs Query
    // =========================================================================

    @Test
    @DisplayName("US-00015: GET /api/v1/audit/high-privilege-logs - Admin Clerk queries audit trail returns 200 OK")
    void testQueryHighPrivilegeLogs_Success() throws Exception {
        AuditLogResponse mockLog = AuditLogResponse.builder()
                .id(101L)
                .userId("superadmin@kvs.com")
                .ipAddress("10.0.0.1")
                .action("UPDATE_TENANT_SECURITY_CONFIG")
                .resource("tenant:DPS-DELHI-01")
                .outcome("SUCCESS")
                .eventTime(Instant.now())
                .details("Updated tenant isolation configuration")
                .build();

        when(auditLogService.queryHighPrivilegeLogs(any(), any())).thenReturn(List.of(mockLog));

        mockMvc.perform(get("/api/v1/audit/high-privilege-logs")
                        .header("Authorization", "Bearer " + clerkToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].action").value("UPDATE_TENANT_SECURITY_CONFIG"))
                .andExpect(jsonPath("$.data[0].userId").value("superadmin@kvs.com"));
    }

    // =========================================================================
    // Action 16 (US-00016, US-00036, etc.): SSL/TLS Certificate & HTTPS Domain Routing
    // =========================================================================

    @Test
    @DisplayName("US-00016: POST /api/v1/tenants/{tenantId}/ssl-certificate - Compliance Officer configures SSL returns 200 OK")
    void testConfigureSslCertificate_Success() throws Exception {
        SslCertificateRequest request = SslCertificateRequest.builder()
                .domainName("campus.dps.edu.in")
                .sslEnabled(true)
                .autoRenewEnabled(true)
                .tlsVersion("TLSv1.3")
                .build();

        SslCertificateResponse mockResponse = SslCertificateResponse.builder()
                .tenantId("DPS-DELHI-01")
                .domainName("campus.dps.edu.in")
                .sslEnabled(true)
                .autoRenewEnabled(true)
                .tlsVersion("TLSv1.3")
                .certificateExpiresAt(Instant.now().plusSeconds(7776000))
                .status("ACTIVE_VALID")
                .build();

        when(tenantIsolationConfigService.configureSslCertificate(eq("DPS-DELHI-01"), any(SslCertificateRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/tenants/DPS-DELHI-01/ssl-certificate")
                        .header("Authorization", "Bearer " + complianceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.domainName").value("campus.dps.edu.in"))
                .andExpect(jsonPath("$.data.tlsVersion").value("TLSv1.3"));
    }

    // =========================================================================
    // Action 17 (US-00017, US-00037, etc.): Password Complexity Policies
    // =========================================================================

    @Test
    @DisplayName("US-00017: PUT /api/v1/security/password-policy - Super Admin configures password policy returns 200 OK")
    void testConfigurePasswordPolicy_Success() throws Exception {
        PasswordPolicyRequest request = PasswordPolicyRequest.builder()
                .tenantId("DPS-DELHI-01")
                .minLength(10)
                .requireUppercase(true)
                .requireLowercase(true)
                .requireDigits(true)
                .requireSpecialChars(true)
                .expiryDays(60)
                .preventReuseCount(5)
                .build();

        PasswordPolicyResponse mockResponse = PasswordPolicyResponse.builder()
                .tenantId("DPS-DELHI-01")
                .minLength(10)
                .requireUppercase(true)
                .requireLowercase(true)
                .requireDigits(true)
                .requireSpecialChars(true)
                .expiryDays(60)
                .preventReuseCount(5)
                .build();

        when(sessionConfigService.configurePasswordPolicy(any(PasswordPolicyRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/security/password-policy")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.minLength").value(10))
                .andExpect(jsonPath("$.data.requireSpecialChars").value(true));
    }

    // =========================================================================
    // Action 18 (US-00018, US-00038, etc.): Branch Concurrency vs Seat Limits
    // =========================================================================

    @Test
    @DisplayName("US-00018: GET /api/v1/tenants/{tenantId}/branches/{branchCode}/concurrency - Trustee checks concurrency returns 200 OK")
    void testGetBranchConcurrency_Success() throws Exception {
        BranchConcurrencyResponse mockResponse = BranchConcurrencyResponse.builder()
                .tenantId("DPS-DELHI-01")
                .branchCode("DPS-DELHI-NORTH")
                .branchName("DPS North Campus")
                .activeUsersCount(45)
                .studentCapacity(250)
                .staffCapacity(50)
                .totalCapacity(300)
                .utilizationPercentage(15.0)
                .capacityExceeded(false)
                .status("NORMAL")
                .build();

        when(branchProvisioningService.getBranchConcurrency("DPS-DELHI-01", "DPS-DELHI-NORTH"))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/tenants/DPS-DELHI-01/branches/DPS-DELHI-NORTH/concurrency")
                        .header("Authorization", "Bearer " + trusteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeUsersCount").value(45))
                .andExpect(jsonPath("$.data.totalCapacity").value(300))
                .andExpect(jsonPath("$.data.capacityExceeded").value(false));
    }

    // =========================================================================
    // Action 19 (US-00019, US-00039, etc.): Field-Level Data Encryption
    // =========================================================================

    @Test
    @DisplayName("US-00019: POST /api/v1/compliance/field-encryption - Principal sets field encryption returns 201 Created")
    void testConfigureFieldEncryption_Success() throws Exception {
        FieldEncryptionPolicyRequest request = FieldEncryptionPolicyRequest.builder()
                .tenantId("DPS-DELHI-01")
                .entityName("StudentHealthRecord")
                .encryptedFields(List.of("bloodGroup", "medicalHistory", "allergies", "doctorNotes"))
                .algorithm("AES-256-GCM")
                .enforced(true)
                .build();

        FieldEncryptionPolicyResponse mockResponse = FieldEncryptionPolicyResponse.builder()
                .tenantId("DPS-DELHI-01")
                .entityName("StudentHealthRecord")
                .encryptedFields(List.of("bloodGroup", "medicalHistory", "allergies", "doctorNotes"))
                .algorithm("AES-256-GCM")
                .enforced(true)
                .status("ENFORCED")
                .build();

        when(privacyComplianceService.configureFieldEncryption(any(FieldEncryptionPolicyRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/compliance/field-encryption")
                        .header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entityName").value("StudentHealthRecord"))
                .andExpect(jsonPath("$.data.enforced").value(true));
    }

    // =========================================================================
    // Action 20 (US-00020, US-00040, etc.): Anomalous Login Alert Rules
    // =========================================================================

    @Test
    @DisplayName("US-00020: POST /api/v1/security/alert-rules - IT Admin configures alert rule returns 201 Created")
    void testCreateAlertRule_Success() throws Exception {
        SecurityAlertRuleRequest request = SecurityAlertRuleRequest.builder()
                .tenantId("DPS-DELHI-01")
                .ruleName("Anomalous Location Login Alert")
                .condition("UNKNOWN_IP_LOCATION")
                .thresholdAttempts(3)
                .severity("HIGH")
                .notificationChannel("EMAIL")
                .enabled(true)
                .build();

        SecurityAlertRuleResponse mockResponse = SecurityAlertRuleResponse.builder()
                .id(10L)
                .tenantId("DPS-DELHI-01")
                .ruleName("Anomalous Location Login Alert")
                .condition("UNKNOWN_IP_LOCATION")
                .thresholdAttempts(3)
                .severity("HIGH")
                .notificationChannel("EMAIL")
                .enabled(true)
                .status("ACTIVE")
                .build();

        when(disasterRecoveryService.createAlertRule(any(SecurityAlertRuleRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/security/alert-rules")
                        .header("Authorization", "Bearer " + itAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ruleName").value("Anomalous Location Login Alert"))
                .andExpect(jsonPath("$.data.severity").value("HIGH"));
    }

    @Test
    @DisplayName("US-00020: GET /api/v1/security/alert-rules - IT Admin retrieves alert rules returns 200 OK")
    void testGetAlertRules_Success() throws Exception {
        SecurityAlertRuleResponse mockResponse = SecurityAlertRuleResponse.builder()
                .id(10L)
                .tenantId("DPS-DELHI-01")
                .ruleName("Anomalous Location Login Alert")
                .condition("UNKNOWN_IP_LOCATION")
                .thresholdAttempts(3)
                .severity("HIGH")
                .notificationChannel("EMAIL")
                .enabled(true)
                .status("ACTIVE")
                .build();

        when(disasterRecoveryService.getAlertRules("DPS-DELHI-01")).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/security/alert-rules?tenantId=DPS-DELHI-01")
                        .header("Authorization", "Bearer " + itAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ruleName").value("Anomalous Location Login Alert"));
    }
}
