package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.SessionConfigRequest;
import com.kvs.auth.dto.SessionConfigResponse;
import com.kvs.auth.service.SessionConfigService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SessionConfigController}.
 * Covers: US-00004: Set global session idle timeout thresholds to 15 minutes
 * Actor: IT System Admin
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private SessionConfigService sessionConfigService;

    private String itAdminToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        itAdminToken = jwtTokenService.generateToken("itadmin@kvs.com", Map.of("roles", List.of("ROLE_IT_SYSTEM_ADMIN")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("PUT /api/v1/security/session-config - IT Admin sets 15 min idle timeout successfully returns 200 OK")
    void testConfigureSession_Success() throws Exception {
        SessionConfigRequest request = new SessionConfigRequest("tenant-001", 15, 480, false, 1);
        SessionConfigResponse response = SessionConfigResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .idleTimeoutMinutes(15)
                .maxSessionDurationMinutes(480)
                .concurrentSessionsAllowed(false)
                .maxConcurrentSessions(1)
                .build();

        when(sessionConfigService.configureSession(any(SessionConfigRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/security/session-config")
                        .header("Authorization", "Bearer " + itAdminToken)
                        .header("X-Trace-Id", "test-trace-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-004"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idleTimeoutMinutes").value(15));
    }

    @Test
    @DisplayName("PUT /api/v1/security/session-config - Non IT admin returns 403 Forbidden")
    void testConfigureSession_Forbidden() throws Exception {
        SessionConfigRequest request = new SessionConfigRequest("tenant-001", 15, 480, false, 1);

        mockMvc.perform(put("/api/v1/security/session-config")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/security/session-config - Invalid zero timeout returns 400 Bad Request")
    void testConfigureSession_ValidationFailed() throws Exception {
        SessionConfigRequest request = new SessionConfigRequest("tenant-001", 0, 480, false, 1);

        mockMvc.perform(put("/api/v1/security/session-config")
                        .header("Authorization", "Bearer " + itAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/security/session-config - Returns current session configuration")
    void testGetSessionConfig_Success() throws Exception {
        SessionConfigResponse response = SessionConfigResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .idleTimeoutMinutes(15)
                .maxSessionDurationMinutes(480)
                .concurrentSessionsAllowed(false)
                .maxConcurrentSessions(1)
                .build();

        when(sessionConfigService.getSessionConfig("tenant-001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/security/session-config?tenantId=tenant-001")
                        .header("Authorization", "Bearer " + itAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idleTimeoutMinutes").value(15));
    }
}
