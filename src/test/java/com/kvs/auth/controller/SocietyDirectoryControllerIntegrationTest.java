package com.kvs.auth.controller;

import com.kvs.auth.entity.BranchProvisioning;
import com.kvs.auth.entity.Tenant;
import com.kvs.auth.entity.User;
import com.kvs.auth.entity.UserCredential;
import com.kvs.auth.entity.UserRole;
import com.kvs.auth.repo.BranchProvisioningRepository;
import com.kvs.auth.repo.TenantRepository;
import com.kvs.auth.repo.UserCredentialRepository;
import com.kvs.auth.repo.UserRepository;
import com.kvs.auth.repo.UserRoleRepository;
import com.kvs.core.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link SocietyDirectoryController}: societies with campuses, and staff with roles and
 * account state, against the real database. Each test creates its own society and is rolled back.
 * Actor: Super Admin
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SocietyDirectoryControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchProvisioningRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private UserCredentialRepository credentialRepository;

    private final String run = UUID.randomUUID().toString().substring(0, 8);
    private final String tenantId = "tenant-dir-" + run;
    private String superAdmin;
    private String registrar;

    @BeforeEach
    void setUp() {
        superAdmin = "Bearer " + jwtTokenService.generateToken("sa_" + run, Map.of("roles", List.of("ROLE_SUPER_ADMIN")));
        registrar = "Bearer " + jwtTokenService.generateToken("reg_" + run, Map.of("roles", List.of("ROLE_REGISTRAR")));

        Tenant t = new Tenant();
        t.setTenantId(tenantId);
        t.setSocietyCode("DIR-" + run);
        t.setSocietyName("Directory Test Society " + run);
        t.setDomainName("dir-" + run + ".test");
        t.setStatus("ACTIVE");
        t.setContactEmail("office@dir-" + run + ".test");
        t.setContactPhone("+911100000000");
        tenantRepository.save(t);
        branchRepository.save(branch("NORTH-" + run, "North Campus"));
        branchRepository.save(branch("SOUTH-" + run, "South Campus"));

        user("registrar_" + run, "NORTH-" + run, "REGISTRAR", true, null, false, "ADMISSION_OFFICER");
        user("teacher_" + run, "SOUTH-" + run, "TEACHER", true, Instant.now().plus(20, ChronoUnit.MINUTES), true);
        user("leaver_" + run, "NORTH-" + run, "TEACHER", false, null, false);
    }

    @Test
    @DisplayName("GET /api/v1/tenants - super admin lists societies with their campuses")
    void listSocieties() throws Exception {
        mockMvc.perform(get("/api/v1/tenants").header("Authorization", superAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.tenantId == '" + tenantId + "')].campuses[*].branchCode",
                        containsInAnyOrder("NORTH-" + run, "SOUTH-" + run)));
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{tenantId}/users - roles and account state, never credentials")
    void listStaff() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/users").header("Authorization", superAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].username").value("leaver_" + run))
                .andExpect(jsonPath("$.data[0].active").value(false))
                .andExpect(jsonPath("$.data[1].username").value("registrar_" + run))
                .andExpect(jsonPath("$.data[1].roles", containsInAnyOrder("ADMISSION_OFFICER", "REGISTRAR")))
                .andExpect(jsonPath("$.data[2].locked").value(true))
                .andExpect(jsonPath("$.data[2].mustChangePassword").value(true))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].passwordSalt").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{tenantId}/users?branchId= - one campus only")
    void listStaffOfCampus() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/users").param("branchId", "SOUTH-" + run).header("Authorization", superAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[*].username", hasItem("teacher_" + run)));
    }

    @Test
    @DisplayName("Directory endpoints - 403 for other roles, 404 for an unknown society, 401 without a token")
    void accessRules() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/users").header("Authorization", registrar)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tenants").header("Authorization", registrar)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tenants/no-such-" + run + "/users").header("Authorization", superAdmin)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/tenants")).andExpect(status().isUnauthorized());
    }

    private BranchProvisioning branch(String code, String name) {
        BranchProvisioning b = new BranchProvisioning();
        b.setTenantId(tenantId);
        b.setBranchCode(code);
        b.setBranchName(name);
        b.setAffiliationBoard("CBSE");
        b.setSchemaName("tenant_" + code.toLowerCase().replace('-', '_'));
        b.setStudentCapacity(1000);
        b.setStaffCapacity(100);
        b.setStatus("PROVISIONED");
        return b;
    }

    private void user(String username, String branchId, String userType, boolean active, Instant lockedUntil,
                      boolean mustChange, String... extraRoles) {
        String userId = "u-" + username;
        User u = new User();
        u.setUserId(userId);
        u.setTenantId(tenantId);
        u.setBranchId(branchId);
        u.setUsername(username);
        u.setEmail(username + "@dir-" + run + ".test");
        u.setUserType(userType);
        u.setFirstName("First");
        u.setLastName("Last");
        u.setActive(active);
        userRepository.save(u);

        UserCredential c = new UserCredential();
        c.setUserId(userId);
        c.setPasswordHash("$2a$10$notarealhashnotarealhashnotarealhashnotarealhashnotare");
        c.setLockedUntil(lockedUntil);
        c.setMustChangePassword(mustChange);
        credentialRepository.save(c);

        for (String role : List.of(userType)) {
            userRoleRepository.save(role(userId, role));
        }
        for (String role : extraRoles) {
            userRoleRepository.save(role(userId, role));
        }
    }

    private UserRole role(String userId, String code) {
        UserRole r = new UserRole();
        r.setUserId(userId);
        r.setRoleCode(code);
        return r;
    }
}
