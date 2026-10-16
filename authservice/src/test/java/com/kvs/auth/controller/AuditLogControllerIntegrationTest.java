package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.AuditLogResponse;
import com.kvs.auth.service.AuditLogService;
import com.kvs.core.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuditLogController}.
 * Covers: US-00005: Query real-time system audit logs filtered by ip address and user id
 * Actor: Security Auditor
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AuditLogService auditLogService;

    private String auditorToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        auditorToken = jwtTokenService.generateToken("auditor@kvs.com", Map.of("roles", List.of("ROLE_SECURITY_AUDITOR")));
        studentToken = jwtTokenService.generateToken("student@kvs.com", Map.of("roles", List.of("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs - Security Auditor queries logs filtered by IP and User ID returns 200 OK")
    void testQueryAuditLogs_Success() throws Exception {
        AuditLogResponse log1 = AuditLogResponse.builder()
                .id(1L)
                .userId("admin_rajesh")
                .ipAddress("192.168.1.50")
                .action("LOGIN_SUCCESS")
                .resource("auth_portal")
                .outcome("SUCCESS")
                .eventTime(Instant.now())
                .details("Successful login")
                .build();

        when(auditLogService.queryAuditLogs(any(), any(), any(), any())).thenReturn(List.of(log1));

        mockMvc.perform(get("/api/v1/audit/logs?userId=admin_rajesh&ipAddress=192.168.1.50")
                        .header("Authorization", "Bearer " + auditorToken)
                        .header("X-Trace-Id", "test-trace-005"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-005"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value("admin_rajesh"))
                .andExpect(jsonPath("$.data[0].ipAddress").value("192.168.1.50"))
                .andExpect(jsonPath("$.data[0].action").value("LOGIN_SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs - Student role returns 403 Forbidden")
    void testQueryAuditLogs_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs - Unauthenticated returns 401 Unauthorized")
    void testQueryAuditLogs_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs"))
                .andExpect(status().isUnauthorized());
    }
}
