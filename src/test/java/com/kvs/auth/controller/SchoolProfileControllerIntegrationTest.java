package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.SchoolProfileRequest;
import com.kvs.auth.entity.Tenant;
import com.kvs.auth.entity.TenantSchoolProfile;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.TenantRepository;
import com.kvs.auth.repo.TenantSchoolProfileRepository;
import com.kvs.core.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link SchoolProfileController}: defaults from the tenant record, save by school administrators, crest upload
 * limited to images. Repositories are mocked so the shared database is not touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SchoolProfileControllerIntegrationTest {

    private static final String TENANT = "tenant-dps";
    private static final String URL = "/api/v1/tenants/{tenantId}/school-profile";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;

    @MockBean private TenantSchoolProfileRepository profileRepository;
    @MockBean private TenantRepository tenantRepository;
    @MockBean private AuditLogRepository auditLogRepository;

    private final AtomicReference<TenantSchoolProfile> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setSocietyName("Delhi Public School Society");
        tenant.setContactEmail("office@dps.edu.in");
        tenant.setContactPhone("+911123456789");
        when(tenantRepository.findByTenantId(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(tenant));
        when(profileRepository.findByTenantId(any())).thenAnswer(i -> Optional.ofNullable(stored.get()));
        when(profileRepository.save(any(TenantSchoolProfile.class))).thenAnswer(i -> { stored.set(i.getArgument(0)); return i.getArgument(0); });
    }

    private String token(String role) {
        return "Bearer " + jwtTokenService.generateToken(role.toLowerCase() + "@dps.com", Map.of("roles", List.of(role), "tenantId", TENANT));
    }

    private static SchoolProfileRequest oakridge() {
        return new SchoolProfileRequest("Oakridge Academy", "Main campus", "office@oakridge.ac.uk", "+44 1865 555 100",
                "OA-00482", "www.oakridge.ac.uk", "12 Oakridge Road, Oxford, OX2 7AA, United Kingdom");
    }

    @Test
    @DisplayName("GET school-profile - unsaved profile comes from the tenant record")
    void get_defaults() throws Exception {
        mockMvc.perform(get(URL, TENANT).header("Authorization", token("ROLE_TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schoolName").value("Delhi Public School Society"))
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.hasCrest").value(false));
    }

    @Test
    @DisplayName("PUT school-profile - administrators save it, teachers are refused, invalid email rejected")
    void save_profile() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", token("ROLE_TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(oakridge())))
                .andExpect(status().isForbidden());

        SchoolProfileRequest bad = oakridge();
        bad.setSchoolEmail("not-an-email");
        mockMvc.perform(put(URL, TENANT).header("Authorization", token("ROLE_SCHOOL_PRINCIPAL"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(URL, TENANT).header("Authorization", token("ROLE_SCHOOL_PRINCIPAL"))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(oakridge())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schoolName").value("Oakridge Academy"))
                .andExpect(jsonPath("$.data.saved").value(true));
    }

    @Test
    @DisplayName("PUT school-profile/crest - PNG accepted and served back, other files refused")
    void crest() throws Exception {
        mockMvc.perform(put(URL, TENANT).header("Authorization", token("ROLE_SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(oakridge()))).andExpect(status().isOk());

        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        mockMvc.perform(multipart(HttpMethod.PUT, URL + "/crest", TENANT)
                        .file(new MockMultipartFile("file", "crest.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", token("ROLE_SUPER_ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(HttpMethod.PUT, URL + "/crest", TENANT)
                        .file(new MockMultipartFile("file", "crest.png", "image/png", png))
                        .header("Authorization", token("ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCrest").value(true));
        mockMvc.perform(get(URL + "/crest", TENANT).header("Authorization", token("ROLE_TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png));
    }
}
