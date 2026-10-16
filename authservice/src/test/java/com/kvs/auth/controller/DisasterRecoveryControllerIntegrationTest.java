package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.DisasterRecoveryBackupRequest;
import com.kvs.auth.dto.DisasterRecoveryBackupResponse;
import com.kvs.auth.service.DisasterRecoveryService;
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
 * Integration tests for {@link DisasterRecoveryController}.
 * Covers: US-00010: Execute automated database snapshot backups to encrypted cloud storage
 * Actor: Trustee
 */
@SpringBootTest
@AutoConfigureMockMvc
class DisasterRecoveryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private DisasterRecoveryService backupService;

    private String trusteeToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        trusteeToken = jwtTokenService.generateToken("trustee@kvs.com", Map.of("roles", List.of("ROLE_TRUSTEE")));
        teacherToken = jwtTokenService.generateToken("teacher@kvs.com", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    @Test
    @DisplayName("POST /api/v1/disaster-recovery/backups - Trustee executes backup returns 201 Created")
    void testExecuteBackup_Success() throws Exception {
        DisasterRecoveryBackupRequest request = new DisasterRecoveryBackupRequest(
                "tenant-001",
                "SNAPSHOT",
                "AWS_S3",
                "kvs-encrypted-backups-mumbai",
                "AES-256-GCM"
        );

        DisasterRecoveryBackupResponse response = DisasterRecoveryBackupResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .snapshotId("snap-20261016-abc123")
                .backupType("SNAPSHOT")
                .storageProvider("AWS_S3")
                .cloudBucket("kvs-encrypted-backups-mumbai")
                .encryptionAlgorithm("AES-256-GCM")
                .sizeBytes(268435456L)
                .status("COMPLETED")
                .backupTimestamp(Instant.now())
                .build();

        when(backupService.executeBackup(any(DisasterRecoveryBackupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/disaster-recovery/backups")
                        .header("Authorization", "Bearer " + trusteeToken)
                        .header("X-Trace-Id", "test-trace-010")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "test-trace-010"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.snapshotId").value("snap-20261016-abc123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.encryptionAlgorithm").value("AES-256-GCM"));
    }

    @Test
    @DisplayName("POST /api/v1/disaster-recovery/backups - Teacher forbidden returns 403")
    void testExecuteBackup_Forbidden() throws Exception {
        DisasterRecoveryBackupRequest request = new DisasterRecoveryBackupRequest(
                "tenant-001", "SNAPSHOT", "AWS_S3", "bucket", "AES-256-GCM"
        );

        mockMvc.perform(post("/api/v1/disaster-recovery/backups")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/disaster-recovery/backups - Returns backup list")
    void testListBackups_Success() throws Exception {
        DisasterRecoveryBackupResponse response = DisasterRecoveryBackupResponse.builder()
                .id(1L)
                .tenantId("tenant-001")
                .snapshotId("snap-20261016-abc123")
                .status("COMPLETED")
                .build();

        when(backupService.listBackups("tenant-001")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/disaster-recovery/backups?tenantId=tenant-001")
                        .header("Authorization", "Bearer " + trusteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].snapshotId").value("snap-20261016-abc123"));
    }
}
