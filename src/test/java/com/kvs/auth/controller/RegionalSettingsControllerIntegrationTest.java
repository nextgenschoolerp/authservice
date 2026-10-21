package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.RegionalSettingsRequest;
import com.kvs.auth.entity.Tenant;
import com.kvs.auth.entity.TenantRegionalSettings;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.TenantRegionalSettingsRepository;
import com.kvs.auth.repo.TenantRepository;
import com.kvs.core.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link RegionalSettingsController}: read by any user of the tenant, save by school
 * administrators, values limited to the UI's option lists, other tenants refused. Repositories are mocked
 * so the shared database is not touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegionalSettingsControllerIntegrationTest {

    private static final String TENANT = "tenant-dps";
    private static final String OTHER_TENANT = "tenant-svkm";
    private static final String URL = "/api/v1/tenants/{tenantId}/regional-settings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private TenantRegionalSettingsRepository settingsRepository;

    @MockBean
    private TenantRepository tenantRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    private String teacherToken;
    private String principalToken;
    private String otherTenantPrincipalToken;
    private String superAdminToken;

    @BeforeEach
    void setUp() {
        teacherToken = token("teacher@dps.com", TENANT, "ROLE_TEACHER");
        principalToken = token("principal@dps.com", TENANT, "ROLE_SCHOOL_PRINCIPAL");
        otherTenantPrincipalToken = token("principal@svkm.com", OTHER_TENANT, "ROLE_SCHOOL_PRINCIPAL");
        superAdminToken = token("superadmin@svkm.com", OTHER_TENANT, "ROLE_SUPER_ADMIN");
        when(tenantRepository.findByTenantId(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(new Tenant()));
        when(tenantRepository.findByTenantId(OTHER_TENANT)).thenReturn(Optional.of(new Tenant()));
        when(settingsRepository.findByTenantId(any())).thenReturn(Optional.empty());
        when(settingsRepository.save(any(TenantRegionalSettings.class))).thenAnswer(i -> i.getArgument(0));
    }

    private String token(String email, String tenantId, String role) {
        return jwtTokenService.generateToken(email, Map.of("roles", List.of(role), "tenantId", tenantId));
    }

    private static RegionalSettingsRequest uk() {
        return new RegionalSettingsRequest("Europe/London", "GBP", "DD/MM/YYYY", "en-GB");
    }

    @Test
    @DisplayName("GET regional-settings - tenant without saved settings returns the defaults to any user")
    void get_defaults() throws Exception {
        mockMvc.perform(get(URL, TENANT).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(TENANT))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.dateFormat").value("DD MMM YYYY"))
                .andExpect(jsonPath("$.data.language").value("en-IN"))
                .andExpect(jsonPath("$.data.saved").value(false));
    }

    @Test
    @DisplayName("GET regional-settings - saved settings are returned")
    void get_saved() throws Exception {
        TenantRegionalSettings saved = new TenantRegionalSettings();
        saved.setTenantId(TENANT);
        saved.setTimeZone("Asia/Dubai");
        saved.setCurrency("AED");
        saved.setDateFormat("YYYY-MM-DD");
        saved.setLanguage("ar-AE");
        when(settingsRepository.findByTenantId(TENANT)).thenReturn(Optional.of(saved));

        mockMvc.perform(get(URL, TENANT).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Dubai"))
                .andExpect(jsonPath("$.data.currency").value("AED"))
                .andExpect(jsonPath("$.data.saved").value(true));
    }

    @Test
    @DisplayName("GET regional-settings - without a token returns 401")
    void get_unauthenticated() throws Exception {
        mockMvc.perform(get(URL, TENANT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET regional-settings - another tenant's settings are refused (403)")
    void get_otherTenant() throws Exception {
        mockMvc.perform(get(URL, TENANT).header("Authorization", "Bearer " + otherTenantPrincipalToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET regional-settings - SUPER_ADMIN may read another tenant; unknown tenant is 404")
    void get_superAdmin() throws Exception {
        mockMvc.perform(get(URL, TENANT).header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(TENANT));
        mockMvc.perform(get(URL, "no-such-tenant").header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT regional-settings - principal saves the tenant's settings and an audit entry is written")
    void put_principal() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Regional preferences saved"))
                .andExpect(jsonPath("$.data.timeZone").value("Europe/London"))
                .andExpect(jsonPath("$.data.currency").value("GBP"))
                .andExpect(jsonPath("$.data.dateFormat").value("DD/MM/YYYY"))
                .andExpect(jsonPath("$.data.language").value("en-GB"))
                .andExpect(jsonPath("$.data.saved").value(true));

        ArgumentCaptor<TenantRegionalSettings> captor = ArgumentCaptor.forClass(TenantRegionalSettings.class);
        verify(settingsRepository).save(captor.capture());
        assertEquals(TENANT, captor.getValue().getTenantId());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("PUT regional-settings - every supported date format and locale is accepted")
    void put_allPatterns() throws Exception {
        for (String format : List.of("DD MMM YYYY", "D MMMM YYYY", "DD/MM/YYYY", "DD-MM-YYYY", "DD.MM.YYYY", "MM/DD/YYYY", "MMM D, YYYY", "YYYY-MM-DD")) {
            mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + principalToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegionalSettingsRequest("UTC", "USD", format, "fr-FR"))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("PUT regional-settings - SUPER_ADMIN may save another tenant's settings")
    void put_superAdmin() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uk())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT regional-settings - a teacher may not save (403)")
    void put_teacherForbidden() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uk())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT regional-settings - a principal of another tenant may not save (403)")
    void put_otherTenantForbidden() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + otherTenantPrincipalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uk())))
                .andExpect(status().isForbidden());
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT regional-settings - values outside the option lists return 400 with one error per field")
    void put_invalidValues() throws Exception {
        RegionalSettingsRequest bad = new RegionalSettingsRequest("Mars/Olympus", "XYZ", "YYYY/DD/MM", "xx-XX");
        mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.length()").value(4));
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT regional-settings - missing fields return 400")
    void put_missingFields() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", "Bearer " + principalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeZone\":\"UTC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
