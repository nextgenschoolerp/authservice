package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.RoleDelegationRequest;
import com.kvs.auth.dto.RoleDelegationResponse;
import com.kvs.auth.service.RoleDelegationService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link RoleDelegationController}.
 * Covers: US-00006: Delegate administrative authority temporarily during executive leave windows
 * Actor: Department Head
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleDelegationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private RoleDelegationService roleDelegationService;

    private String deptHeadToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        deptHeadToken = jwtTokenService.generateToken("depthead@kvs.com", Map.of("roles", List.of("ROLE_DEPARTMENT_HEAD")));
        studentToken = jwtTokenService.generateToken("student@kvs.com", Map.of("roles", List.of("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("POST /api/v1/roles/delegations - Department Head delegates role authority successfully returns 201 Created")
    void testDelegateAuthority_Success() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(7, ChronoUnit.DAYS);

        RoleDelegationRequest request = new RoleDelegationRequest(
                "tenant-001",
                "user-dept-head-01",
                "user-deputy-02",
                10L,
                "ACADEMIC_ADMIN",
                start,
                end,
                "Medical leave"
        );

        RoleDelegationResponse response = RoleDelegationResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .delegatorUserId("user-dept-head-01")
                .delegateeUserId("user-deputy-02")
                .roleId(10L)
                .roleName("ACADEMIC_ADMIN")
                .startTime(start)
                .endTime(end)
                .reason("Medical leave")
                .status("ACTIVE")
                .build();

        when(roleDelegationService.delegateAuthority(any(RoleDelegationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/roles/delegations")
                        .header("Authorization", "Bearer " + deptHeadToken)
                        .header("X-Trace-Id", "test-trace-006")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "test-trace-006"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleName").value("ACADEMIC_ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/roles/delegations - Student forbidden returns 403")
    void testDelegateAuthority_Forbidden() throws Exception {
        RoleDelegationRequest request = new RoleDelegationRequest(
                "tenant-001", "u1", "u2", 10L, "ROLE",
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(7, ChronoUnit.DAYS), "Leave"
        );

        mockMvc.perform(post("/api/v1/roles/delegations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/delegations/{id} - Revoke delegation returns 200 OK")
    void testRevokeDelegation_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/delegations/1")
                        .header("Authorization", "Bearer " + deptHeadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
