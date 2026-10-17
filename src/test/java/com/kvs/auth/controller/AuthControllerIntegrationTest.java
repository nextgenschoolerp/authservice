package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.AuthResponse;
import com.kvs.auth.dto.LoginRequest;
import com.kvs.auth.dto.SuperAdminRegisterRequest;
import com.kvs.auth.dto.SuperAdminRegisterResponse;
import com.kvs.auth.service.AuthService;
import com.kvs.core.dto.ApiErrorResponse;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.UnauthorizedException;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 * Covers:
 * 1. Super Admin Self-Registration and Society Onboarding (POST /api/v1/auth/register)
 * 2. Super Admin Authentication / Login with Lockout Verification (POST /api/v1/auth/login)
 * 3. Principal Profile Verification (GET /api/v1/auth/me)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AuthService authService;

    private String superAdminToken;

    @BeforeEach
    void setUp() {
        superAdminToken = jwtTokenService.generateToken("admin_rajesh", Map.of("roles", List.of("ROLE_SUPER_ADMIN")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Valid Super Admin registration returns 201 Created")
    void testRegisterSuperAdmin_Success() throws Exception {
        SuperAdminRegisterRequest request = SuperAdminRegisterRequest.builder()
                .societyCode("DPS-SOC-01")
                .societyName("Delhi Public Education Society")
                .domainName("dps.edu.in")
                .contactEmail("contact@dps.edu.in")
                .contactPhone("+911123456789")
                .username("admin_rajesh")
                .email("rajesh.admin@dps.edu.in")
                .password("SecureAdmin@2026")
                .firstName("Rajesh")
                .lastName("Sharma")
                .phoneNumber("+919876543210")
                .branchCode("DPS-DELHI-NORTH")
                .branchName("DPS North Campus")
                .affiliationBoard("CBSE")
                .planTier("PROFESSIONAL")
                .build();

        SuperAdminRegisterResponse response = SuperAdminRegisterResponse.builder()
                .tenantId("tenant-uuid-101")
                .societyCode("DPS-SOC-01")
                .societyName("Delhi Public Education Society")
                .userId("user-uuid-202")
                .username("admin_rajesh")
                .email("rajesh.admin@dps.edu.in")
                .userType("SUPER_ADMIN")
                .roles(List.of("ROLE_SUPER_ADMIN"))
                .status("ACTIVE")
                .message("Super Admin and Educational Society onboarded successfully")
                .createdAt(Instant.now())
                .build();

        when(authService.registerSuperAdmin(any(SuperAdminRegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-uuid-101"))
                .andExpect(jsonPath("$.data.societyCode").value("DPS-SOC-01"))
                .andExpect(jsonPath("$.data.username").value("admin_rajesh"))
                .andExpect(jsonPath("$.data.userType").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_SUPER_ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Duplicate society code returns 409 Conflict")
    void testRegisterSuperAdmin_DuplicateConflict() throws Exception {
        SuperAdminRegisterRequest request = SuperAdminRegisterRequest.builder()
                .societyCode("DPS-SOC-01")
                .societyName("Delhi Public Education Society")
                .contactEmail("contact@dps.edu.in")
                .contactPhone("+911123456789")
                .username("admin_rajesh")
                .email("rajesh.admin@dps.edu.in")
                .password("SecureAdmin@2026")
                .firstName("Rajesh")
                .lastName("Sharma")
                .phoneNumber("+919876543210")
                .build();

        when(authService.registerSuperAdmin(any(SuperAdminRegisterRequest.class)))
                .thenThrow(new ConflictException("Society with code 'DPS-SOC-01' is already registered"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"))
                .andReturn();

        ApiErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ApiErrorResponse.class);
        assertEquals("Society with code 'DPS-SOC-01' is already registered", error.getMessage());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Validation failure on missing fields returns 400 Bad Request")
    void testRegisterSuperAdmin_ValidationFailure() throws Exception {
        SuperAdminRegisterRequest invalidRequest = new SuperAdminRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Valid Super Admin credentials returns 200 OK with JWT token")
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin_rajesh")
                .password("SecureAdmin@2026")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock-jwt-token-string")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId("user-uuid-202")
                .username("admin_rajesh")
                .email("rajesh.admin@dps.edu.in")
                .tenantId("tenant-uuid-101")
                .userType("SUPER_ADMIN")
                .roles(List.of("ROLE_SUPER_ADMIN"))
                .mfaRequired(false)
                .sessionId("session-uuid-303")
                .message("Login successful")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token-string"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("admin_rajesh"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.sessionId").value("session-uuid-303"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Incorrect password returns 401 Unauthorized")
    void testLogin_IncorrectPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin_rajesh")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Account locked after 5 failed attempts returns 401 with lockout message")
    void testLogin_AccountLocked() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin_rajesh")
                .password("AnyPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Account is locked due to 5 consecutive failed login attempts. Locked until: 2026-10-17T00:00:00Z"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is locked due to 5 consecutive failed login attempts. Locked until: 2026-10-17T00:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Authenticated request returns 200 OK and user profile")
    void testGetCurrentUser_Success() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .userId("user-uuid-202")
                .username("admin_rajesh")
                .email("rajesh.admin@dps.edu.in")
                .tenantId("tenant-uuid-101")
                .userType("SUPER_ADMIN")
                .roles(List.of("ROLE_SUPER_ADMIN"))
                .message("User profile retrieved successfully")
                .build();

        when(authService.getCurrentUserProfile("admin_rajesh")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin_rajesh"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_SUPER_ADMIN"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Unauthenticated request returns 401 Unauthorized")
    void testGetCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
