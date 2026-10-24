package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.BootstrapRequest;
import com.kvs.auth.dto.BootstrapResponse;
import com.kvs.auth.dto.BootstrapStatusResponse;
import com.kvs.auth.service.AuthService;
import com.kvs.auth.service.BootstrapService;
import com.kvs.core.exception.ConflictException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Zero-State System Bootstrap endpoints in {@link AuthController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BootstrapControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private BootstrapService bootstrapService;

    @Test
    @DisplayName("GET /api/v1/auth/bootstrap/status - Returns bootstrap required status when DB is clean")
    void testGetBootstrapStatus_CleanDb() throws Exception {
        BootstrapStatusResponse status = BootstrapStatusResponse.builder()
                .initialized(false)
                .bootstrapRequired(true)
                .tenantCount(0)
                .userCount(0)
                .systemVersion("2.0.0-PROD")
                .build();

        when(bootstrapService.getBootstrapStatus()).thenReturn(status);

        mockMvc.perform(get("/api/v1/auth/bootstrap/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.initialized").value(false))
                .andExpect(jsonPath("$.data.bootstrapRequired").value(true))
                .andExpect(jsonPath("$.data.tenantCount").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/auth/bootstrap/template - Streams valid Excel .xlsx onboarding workbook")
    void testDownloadBootstrapTemplate() throws Exception {
        // Generate real template bytes using a temporary workbook
        byte[] excelBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet("1_Organization");
            wb.createSheet("2_Primary_Campus");
            wb.createSheet("3_IT_Administrator");
            wb.write(out);
            excelBytes = out.toByteArray();
        }

        when(bootstrapService.generateTemplate()).thenReturn(excelBytes);

        MvcResult result = mockMvc.perform(get("/api/v1/auth/bootstrap/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"School_ERP_Initial_Setup_Template.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertTrue(body.length > 0, "Excel template should not be empty");

        // Verify valid POI workbook parsing
        try (Workbook parsedWb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertEquals(3, parsedWb.getNumberOfSheets());
            assertEquals("1_Organization", parsedWb.getSheetName(0));
            assertEquals("2_Primary_Campus", parsedWb.getSheetName(1));
            assertEquals("3_IT_Administrator", parsedWb.getSheetName(2));
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/bootstrap - JSON direct onboarding success")
    void testBootstrapJson_Success() throws Exception {
        BootstrapRequest request = BootstrapRequest.builder()
                .societyName("Delhi Public School Society")
                .societyCode("DPS-HQ")
                .domainName("dps.edu.in")
                .curriculumBoard("CBSE")
                .contactEmail("admin@dps.edu.in")
                .contactPhone("+91-11-23456789")
                .branchName("DPS North Delhi Campus")
                .branchCode("DPS-DELHI-NORTH")
                .city("New Delhi")
                .state("Delhi")
                .address("Sector 14, Rohini")
                .pincode("110085")
                .adminUsername("itadmin")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("rajesh.it@dps.edu.in")
                .adminPhone("+91-9876543210")
                .temporaryPassword("Welcome@2026!")
                .build();

        BootstrapResponse response = BootstrapResponse.builder()
                .tenantId("tenant-uuid-123")
                .societyCode("DPS-HQ")
                .societyName("Delhi Public School Society")
                .branchCode("DPS-DELHI-NORTH")
                .branchName("DPS North Delhi Campus")
                .userId("user-uuid-456")
                .username("itadmin")
                .email("rajesh.it@dps.edu.in")
                .temporaryPassword("Welcome@2026!")
                .mustChangePassword(true)
                .message("Initial system setup completed.")
                .build();

        when(bootstrapService.bootstrap(any(BootstrapRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("itadmin"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value("Welcome@2026!"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/bootstrap/upload - Excel multipart upload onboarding success")
    void testBootstrapUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "School_ERP_Initial_Setup_Template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy-excel-bytes".getBytes()
        );

        BootstrapResponse response = BootstrapResponse.builder()
                .tenantId("tenant-uuid-123")
                .societyCode("DPS-HQ")
                .societyName("Delhi Public School Society")
                .branchCode("DPS-DELHI-NORTH")
                .branchName("DPS North Delhi Campus")
                .userId("user-uuid-456")
                .username("itadmin")
                .email("rajesh.it@dps.edu.in")
                .temporaryPassword("Init@2026abcd")
                .mustChangePassword(true)
                .message("Initial system setup completed from Excel.")
                .build();

        when(bootstrapService.bootstrapFromUpload(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/auth/bootstrap/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("itadmin"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/bootstrap - Returns 409 Conflict when system is already initialized")
    void testBootstrap_ConflictWhenAlreadyInitialized() throws Exception {
        BootstrapRequest request = BootstrapRequest.builder()
                .societyName("Duplicate Society")
                .societyCode("DPS-HQ")
                .contactEmail("admin@dps.edu.in")
                .contactPhone("+91-11-23456789")
                .branchName("DPS North")
                .branchCode("DPS-NORTH")
                .adminUsername("itadmin")
                .adminFirstName("Rajesh")
                .adminLastName("Sharma")
                .adminEmail("rajesh.it@dps.edu.in")
                .build();

        when(bootstrapService.bootstrap(any(BootstrapRequest.class)))
                .thenThrow(new ConflictException("System is already initialized. Bootstrap is permanently locked."));

        mockMvc.perform(post("/api/v1/auth/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("System is already initialized. Bootstrap is permanently locked."));
    }
}
