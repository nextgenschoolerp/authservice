package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.AnonymizeProfileRequest;
import com.kvs.auth.dto.PrivacyAnonymizationResponse;
import com.kvs.auth.service.PrivacyComplianceService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link PrivacyComplianceController}.
 * Covers: US-00008: Anonymize inactive student and staff profiles to comply with privacy laws
 * Actor: Compliance Officer
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrivacyComplianceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private PrivacyComplianceService complianceService;

    private String complianceOfficerToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        complianceOfficerToken = jwtTokenService.generateToken("compliance@kvs.com", Map.of("roles", List.of("ROLE_COMPLIANCE_OFFICER")));
        studentToken = jwtTokenService.generateToken("student@kvs.com", Map.of("roles", List.of("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("POST /api/v1/compliance/anonymize - Compliance Officer anonymizes profile returns 200 OK")
    void testAnonymizeProfile_Success() throws Exception {
        AnonymizeProfileRequest request = new AnonymizeProfileRequest(
                "tenant-001",
                "inactive-student-99",
                "STUDENT",
                "RIGHT_TO_BE_FORGOTTEN",
                "email,phone,address",
                "Alumni graduated 7 years ago"
        );

        PrivacyAnonymizationResponse response = PrivacyAnonymizationResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .userId("inactive-student-99")
                .profileType("STUDENT")
                .requestType("RIGHT_TO_BE_FORGOTTEN")
                .anonymizedFields("email,phone,address")
                .reason("Alumni graduated 7 years ago")
                .status("COMPLETED")
                .executedBy("compliance@kvs.com")
                .executedAt(Instant.now())
                .build();

        when(complianceService.anonymizeProfile(any(AnonymizeProfileRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/compliance/anonymize")
                        .header("Authorization", "Bearer " + complianceOfficerToken)
                        .header("X-Trace-Id", "test-trace-008")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-008"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.userId").value("inactive-student-99"));
    }

    @Test
    @DisplayName("POST /api/v1/compliance/anonymize - Student role returns 403 Forbidden")
    void testAnonymizeProfile_Forbidden() throws Exception {
        AnonymizeProfileRequest request = new AnonymizeProfileRequest(
                "tenant-001", "u1", "STUDENT", "FORGET", "email", "reason"
        );

        mockMvc.perform(post("/api/v1/compliance/anonymize")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/compliance/anonymization-logs - Returns anonymization logs")
    void testGetAnonymizationLogs_Success() throws Exception {
        PrivacyAnonymizationResponse log1 = PrivacyAnonymizationResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .userId("inactive-student-99")
                .profileType("STUDENT")
                .status("COMPLETED")
                .build();

        when(complianceService.getAnonymizationLogs("tenant-001")).thenReturn(List.of(log1));

        mockMvc.perform(get("/api/v1/compliance/anonymization-logs?tenantId=tenant-001")
                        .header("Authorization", "Bearer " + complianceOfficerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value("inactive-student-99"));
    }
}
