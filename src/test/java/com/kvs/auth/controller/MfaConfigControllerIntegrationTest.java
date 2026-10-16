package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.MfaConfigRequest;
import com.kvs.auth.dto.MfaConfigResponse;
import com.kvs.auth.service.MfaConfigService;
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
 * Integration tests for {@link MfaConfigController}.
 * Covers: US-00003: Enforce mandatory totp/sms multi-factor authentication for sensitive transactions
 * Actor: Principal
 */
@SpringBootTest
@AutoConfigureMockMvc
class MfaConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private MfaConfigService mfaConfigService;

    private String principalToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        principalToken = jwtTokenService.generateToken("principal@kvs.com", Map.of("roles", List.of("ROLE_PRINCIPAL")));
        studentToken = jwtTokenService.generateToken("student@kvs.com", Map.of("roles", List.of("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("PUT /api/v1/mfa/config - Principal configures MFA policy successfully returns 200 OK")
    void testConfigureMfa_Success() throws Exception {
        MfaConfigRequest request = new MfaConfigRequest("tenant-001", true, true, true, 5);
        MfaConfigResponse response = MfaConfigResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .totpEnabled(true)
                .smsEnabled(true)
                .mandatoryForSensitiveTransactions(true)
                .maxFailedAttempts(5)
                .build();

        when(mfaConfigService.configureMfa(any(MfaConfigRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/mfa/config")
                        .header("Authorization", "Bearer " + principalToken)
                        .header("X-Trace-Id", "test-trace-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-003"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totpEnabled").value(true))
                .andExpect(jsonPath("$.data.mandatoryForSensitiveTransactions").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/mfa/config - Student forbidden returns 403")
    void testConfigureMfa_Forbidden() throws Exception {
        MfaConfigRequest request = new MfaConfigRequest("tenant-001", true, true, true, 5);

        mockMvc.perform(put("/api/v1/mfa/config")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/mfa/config - Returns MFA policy")
    void testGetMfaConfig_Success() throws Exception {
        MfaConfigResponse response = MfaConfigResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .totpEnabled(true)
                .smsEnabled(true)
                .mandatoryForSensitiveTransactions(true)
                .maxFailedAttempts(5)
                .build();

        when(mfaConfigService.getMfaConfig("tenant-001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/mfa/config?tenantId=tenant-001")
                        .header("Authorization", "Bearer " + principalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-001"));
    }
}
