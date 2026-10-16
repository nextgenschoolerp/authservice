package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.BranchProvisioningRequest;
import com.kvs.auth.dto.BranchProvisioningResponse;
import com.kvs.auth.service.BranchProvisioningService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link BranchProvisioningController}.
 * Covers: US-00009: Provision isolated database schemas for newly onboarded school branches
 * Actor: Super Admin
 */
@SpringBootTest
@AutoConfigureMockMvc
class BranchProvisioningControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private BranchProvisioningService branchProvisioningService;

    private String superAdminToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        superAdminToken = jwtTokenService.generateToken("superadmin@kvs.com", Map.of("roles", List.of("ROLE_SUPER_ADMIN")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("POST /api/v1/tenants/{tenantId}/branches/provision - Super Admin provisions branch schema returns 201 Created")
    void testProvisionBranch_Success() throws Exception {
        BranchProvisioningRequest request = new BranchProvisioningRequest(
                "DPS-EAST-01",
                "DPS East Campus",
                "CBSE",
                "schema_dps_east",
                3000,
                200,
                true
        );

        BranchProvisioningResponse response = BranchProvisioningResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .branchCode("DPS-EAST-01")
                .branchName("DPS East Campus")
                .affiliationBoard("CBSE")
                .schemaName("schema_dps_east")
                .studentCapacity(3000)
                .staffCapacity(200)
                .isolatedDbEnabled(true)
                .status("PROVISIONED")
                .createdAt(Instant.now())
                .build();

        when(branchProvisioningService.provisionBranch(eq("tenant-001"), any(BranchProvisioningRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/tenants/tenant-001/branches/provision")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .header("X-Trace-Id", "test-trace-009")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "test-trace-009"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("DPS-EAST-01"))
                .andExpect(jsonPath("$.data.schemaName").value("schema_dps_east"))
                .andExpect(jsonPath("$.data.isolatedDbEnabled").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/tenants/{tenantId}/branches/provision - Teacher forbidden returns 403")
    void testProvisionBranch_Forbidden() throws Exception {
        BranchProvisioningRequest request = new BranchProvisioningRequest(
                "DPS-EAST-01", "DPS East", "CBSE", "schema_east", 1000, 50, true
        );

        mockMvc.perform(post("/api/v1/tenants/tenant-001/branches/provision")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{tenantId}/branches/{branchCode} - Returns branch details")
    void testGetBranch_Success() throws Exception {
        BranchProvisioningResponse response = BranchProvisioningResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .branchCode("DPS-EAST-01")
                .branchName("DPS East Campus")
                .schemaName("schema_dps_east")
                .status("PROVISIONED")
                .build();

        when(branchProvisioningService.getBranch("tenant-001", "DPS-EAST-01")).thenReturn(response);

        mockMvc.perform(get("/api/v1/tenants/tenant-001/branches/DPS-EAST-01")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("DPS-EAST-01"));
    }
}
