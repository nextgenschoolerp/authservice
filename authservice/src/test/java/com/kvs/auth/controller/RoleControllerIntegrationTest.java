package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.*;
import com.kvs.auth.service.RoleService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link RoleController}.
 * Covers: US-00002: Assign screen-level read/write/delete/export permissions to custom roles
 * Actor: Trustee
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private RoleService roleService;

    private String trusteeToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        trusteeToken = jwtTokenService.generateToken("trustee@kvs.com", Map.of("roles", List.of("ROLE_TRUSTEE")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("POST /api/v1/roles/{roleId}/permissions - Trustee assigns screen permissions returns 200 OK")
    void testAssignPermissions_Success() throws Exception {
        RolePermissionRequest permReq = new RolePermissionRequest("STUDENT_PROFILE", true, true, false, true);
        AssignScreenPermissionsRequest request = new AssignScreenPermissionsRequest(List.of(permReq));

        RolePermissionResponse permResp = RolePermissionResponse.builder()
                .id(1L)
                .screenName("STUDENT_PROFILE")
                .canRead(true)
                .canWrite(true)
                .canDelete(false)
                .canExport(true)
                .build();

        when(roleService.assignPermissions(eq(10L), any()))
                .thenReturn(List.of(permResp));

        mockMvc.perform(post("/api/v1/roles/10/permissions")
                        .header("Authorization", "Bearer " + trusteeToken)
                        .header("X-Trace-Id", "test-trace-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-002"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].screenName").value("STUDENT_PROFILE"))
                .andExpect(jsonPath("$.data[0].canRead").value(true))
                .andExpect(jsonPath("$.data[0].canExport").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/roles/{roleId}/permissions - Non-trustee returns 403 Forbidden")
    void testAssignPermissions_Forbidden() throws Exception {
        RolePermissionRequest permReq = new RolePermissionRequest("STUDENT_PROFILE", true, true, false, true);
        AssignScreenPermissionsRequest request = new AssignScreenPermissionsRequest(List.of(permReq));

        mockMvc.perform(post("/api/v1/roles/10/permissions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/roles/{roleId}/permissions - Unauthenticated returns 401 Unauthorized")
    void testAssignPermissions_Unauthorized() throws Exception {
        RolePermissionRequest permReq = new RolePermissionRequest("STUDENT_PROFILE", true, true, false, true);
        AssignScreenPermissionsRequest request = new AssignScreenPermissionsRequest(List.of(permReq));

        mockMvc.perform(post("/api/v1/roles/10/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/roles - Empty name returns 400 Bad Request")
    void testCreateRole_ValidationError() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("", "Invalid role");

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + trusteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{roleId}/permissions - Returns permissions list")
    void testGetPermissions_Success() throws Exception {
        RolePermissionResponse permResp = RolePermissionResponse.builder()
                .id(1L)
                .screenName("STUDENT_PROFILE")
                .canRead(true)
                .canWrite(true)
                .canDelete(false)
                .canExport(true)
                .build();

        when(roleService.getPermissions(10L)).thenReturn(List.of(permResp));

        mockMvc.perform(get("/api/v1/roles/10/permissions")
                        .header("Authorization", "Bearer " + trusteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].screenName").value("STUDENT_PROFILE"));
    }
}
