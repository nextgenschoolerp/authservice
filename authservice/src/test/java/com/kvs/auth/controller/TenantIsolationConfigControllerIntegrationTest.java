package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.TenantIsolationConfigRequest;
import com.kvs.auth.dto.TenantIsolationConfigResponse;
import com.kvs.auth.service.TenantIsolationConfigService;
import com.kvs.core.dto.ApiErrorResponse;
import com.kvs.core.exception.ResourceNotFoundException;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link TenantIsolationConfigController}.
 *
 * Covers:
 * US-00001: Configure multi-tenant database isolation parameters - Scenario 1
 */
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private TenantIsolationConfigService tenantIsolationConfigService;

    private String superAdminToken;
    private String regularUserToken;

    @BeforeEach
    void setUp() {
        superAdminToken = jwtTokenService.generateToken("superadmin@kvs.com", Map.of("roles", List.of("ROLE_SUPER_ADMIN")));
        regularUserToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("PUT /api/v1/tenants/{tenantId}/isolation-config - Valid request by Super Admin returns 200 OK")
    void testConfigureIsolation_Success() throws Exception {
        TenantIsolationConfigRequest request = new TenantIsolationConfigRequest(
                "school_branch_01",
                true,
                "AES-256-GCM",
                true
        );

        TenantIsolationConfigResponse response = TenantIsolationConfigResponse.builder()
                .id(1L)
                .tenantId("branch-north-01")
                .schemaName("school_branch_01")
                .crossTenantAccessBlocked(true)
                .encryptionAlgorithm("AES-256-GCM")
                .rowLevelSecurityEnabled(true)
                .updatedAt(Instant.now())
                .build();

        when(tenantIsolationConfigService.configureIsolation(eq("branch-north-01"), any(TenantIsolationConfigRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/tenants/branch-north-01/isolation-config")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tenant database isolation configured successfully"))
                .andExpect(jsonPath("$.data.tenantId").value("branch-north-01"))
                .andExpect(jsonPath("$.data.schemaName").value("school_branch_01"))
                .andExpect(jsonPath("$.data.crossTenantAccessBlocked").value(true))
                .andExpect(jsonPath("$.data.encryptionAlgorithm").value("AES-256-GCM"))
                .andExpect(jsonPath("$.data.rowLevelSecurityEnabled").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/tenants/{tenantId}/isolation-config - Blank schemaName returns 400 Bad Request")
    void testConfigureIsolation_ValidationError() throws Exception {
        TenantIsolationConfigRequest invalidRequest = new TenantIsolationConfigRequest(
                "", // Blank schemaName violates @NotBlank
                true,
                "AES-256",
                true
        );

        mockMvc.perform(put("/api/v1/tenants/branch-north-01/isolation-config")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    @DisplayName("PUT /api/v1/tenants/{tenantId}/isolation-config - Unauthenticated returns 401 Unauthorized")
    void testConfigureIsolation_Unauthenticated() throws Exception {
        TenantIsolationConfigRequest request = new TenantIsolationConfigRequest(
                "school_branch_01",
                true,
                "AES-256",
                true
        );

        MvcResult result = mockMvc.perform(put("/api/v1/tenants/branch-north-01/isolation-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        String headerTraceId = result.getResponse().getHeader("X-Trace-Id");
        ApiErrorResponse body = objectMapper.readValue(result.getResponse().getContentAsString(), ApiErrorResponse.class);
        assertNotNull(body.getTraceId());
        assertEquals(headerTraceId, body.getTraceId());
    }

    @Test
    @DisplayName("PUT /api/v1/tenants/{tenantId}/isolation-config - Non Super Admin returns 403 Forbidden")
    void testConfigureIsolation_ForbiddenForNonSuperAdmin() throws Exception {
        TenantIsolationConfigRequest request = new TenantIsolationConfigRequest(
                "school_branch_01",
                true,
                "AES-256",
                true
        );

        mockMvc.perform(put("/api/v1/tenants/branch-north-01/isolation-config")
                        .header("Authorization", "Bearer " + regularUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{tenantId}/isolation-config - Existing tenant returns 200 OK")
    void testGetIsolationConfig_Success() throws Exception {
        TenantIsolationConfigResponse response = TenantIsolationConfigResponse.builder()
                .id(1L)
                .tenantId("branch-north-01")
                .schemaName("school_branch_01")
                .crossTenantAccessBlocked(true)
                .encryptionAlgorithm("AES-256-GCM")
                .rowLevelSecurityEnabled(true)
                .updatedAt(Instant.now())
                .build();

        when(tenantIsolationConfigService.getIsolationConfig("branch-north-01"))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/tenants/branch-north-01/isolation-config")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value("branch-north-01"))
                .andExpect(jsonPath("$.data.schemaName").value("school_branch_01"));
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{tenantId}/isolation-config - Non-existent tenant returns 404 Not Found")
    void testGetIsolationConfig_NotFound() throws Exception {
        when(tenantIsolationConfigService.getIsolationConfig("unknown-branch"))
                .thenThrow(new ResourceNotFoundException("TenantIsolationConfig", "tenantId", "unknown-branch"));

        MvcResult result = mockMvc.perform(get("/api/v1/tenants/unknown-branch/isolation-config")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        String headerTraceId = result.getResponse().getHeader("X-Trace-Id");
        ApiErrorResponse body = objectMapper.readValue(result.getResponse().getContentAsString(), ApiErrorResponse.class);
        assertNotNull(body.getTraceId());
        assertEquals(headerTraceId, body.getTraceId());
    }

    @Test
    @DisplayName("Header Propagation - Inbound X-Trace-Id and X-Span-Id are preserved in response")
    void testTraceHeaderPropagation() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/branch-north-01/isolation-config")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Trace-Id", "custom-trace-10001")
                        .header("X-Span-Id", "custom-span-20002"))
                .andExpect(header().string("X-Trace-Id", "custom-trace-10001"))
                .andExpect(header().string("X-Span-Id", "custom-span-20002"));
    }
}
