package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.ApiTokenResponse;
import com.kvs.auth.dto.GenerateApiTokenRequest;
import com.kvs.auth.service.ApiTokenService;
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
 * Integration tests for {@link ApiTokenController}.
 * Covers: US-00007: Generate encrypted oauth2 api tokens for third-party software integrations
 * Actor: Admin Clerk
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTokenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private ApiTokenService apiTokenService;

    private String adminClerkToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        adminClerkToken = jwtTokenService.generateToken("clerk@kvs.com", Map.of("roles", List.of("ROLE_ADMIN_CLERK")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("POST /api/v1/integrations/api-tokens - Admin Clerk generates OAuth2 API token returns 201 Created")
    void testGenerateApiToken_Success() throws Exception {
        GenerateApiTokenRequest request = new GenerateApiTokenRequest(
                "tenant-001",
                "vendor-partner-01",
                "AttendanceSyncService",
                "read:students,write:attendance",
                120,
                30
        );

        ApiTokenResponse response = ApiTokenResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .userId("vendor-partner-01")
                .tokenName("AttendanceSyncService")
                .tokenValue("kvs_secret_token_12345")
                .scopes("read:students,write:attendance")
                .rateLimitRpm(120)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .build();

        when(apiTokenService.generateApiToken(any(GenerateApiTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/integrations/api-tokens")
                        .header("Authorization", "Bearer " + adminClerkToken)
                        .header("X-Trace-Id", "test-trace-007")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "test-trace-007"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenValue").value("kvs_secret_token_12345"))
                .andExpect(jsonPath("$.data.rateLimitRpm").value(120));
    }

    @Test
    @DisplayName("POST /api/v1/integrations/api-tokens - Teacher forbidden returns 403")
    void testGenerateApiToken_Forbidden() throws Exception {
        GenerateApiTokenRequest request = new GenerateApiTokenRequest(
                "tenant-001", "u1", "tok", "read:data", 100, 30
        );

        mockMvc.perform(post("/api/v1/integrations/api-tokens")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/integrations/api-tokens/{id} - Revoke token returns 200 OK")
    void testRevokeApiToken_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/integrations/api-tokens/1")
                        .header("Authorization", "Bearer " + adminClerkToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
