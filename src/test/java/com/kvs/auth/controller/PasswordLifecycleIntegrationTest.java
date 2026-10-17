package com.kvs.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.*;
import com.kvs.auth.service.AuthService;
import com.kvs.core.exception.BadRequestException;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Password Lifecycle Management endpoints:
 * US-01001: Self-Service Forgot Password
 * US-01002: Reset Password via Token
 * US-01003: Authenticated User Password Change
 * US-01004: Mandatory First-Time Login Password Change
 * US-01005: Administrative Account Unlock & Temporary Password Reset
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AuthService authService;

    private String superAdminToken;
    private String principalToken;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        superAdminToken = jwtTokenService.generateToken("admin_rajesh", Map.of("roles", List.of("ROLE_SUPER_ADMIN")));
        principalToken = jwtTokenService.generateToken("principal_anita", Map.of("roles", List.of("ROLE_PRINCIPAL")));
        teacherToken = jwtTokenService.generateToken("teacher_vikram", Map.of("roles", List.of("ROLE_TEACHER")));
    }

    // ========================================================================
    // US-01001: Forgot Password
    // ========================================================================

    @Test
    @DisplayName("US-01001: POST /api/v1/auth/forgot-password - Valid user returns 200 OK with reset token")
    void testForgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .identifier("teacher_vikram")
                .tenantId("tenant-101")
                .build();

        ForgotPasswordResponse response = ForgotPasswordResponse.builder()
                .message("If an account matches your details, a reset notification has been sent")
                .resetToken("test-token-hash-12345")
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resetToken").value("test-token-hash-12345"))
                .andExpect(jsonPath("$.data.message").value("If an account matches your details, a reset notification has been sent"));
    }

    @Test
    @DisplayName("US-01001: POST /api/v1/auth/forgot-password - Blank identifier fails validation with 400 Bad Request")
    void testForgotPassword_ValidationFailure() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .identifier("")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========================================================================
    // US-01002: Reset Password
    // ========================================================================

    @Test
    @DisplayName("US-01002: POST /api/v1/auth/reset-password - Valid token and compliant password succeeds")
    void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token-123")
                .newPassword("NewSecurePassword@2026")
                .confirmPassword("NewSecurePassword@2026")
                .build();

        AuthResponse response = AuthResponse.builder()
                .userId("u-101")
                .username("teacher_vikram")
                .email("vikram@dps.edu.in")
                .message("Password has been reset successfully")
                .build();

        when(authService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("teacher_vikram"))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));
    }

    @Test
    @DisplayName("US-01002: POST /api/v1/auth/reset-password - Expired token throws 400 Bad Request")
    void testResetPassword_ExpiredToken() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("NewSecurePassword@2026")
                .confirmPassword("NewSecurePassword@2026")
                .build();

        when(authService.resetPassword(any(ResetPasswordRequest.class)))
                .thenThrow(new BadRequestException("Invalid or expired password reset token"));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset token"));
    }

    @Test
    @DisplayName("US-01002: POST /api/v1/auth/reset-password - Weak password fails complexity validation with 400")
    void testResetPassword_WeakPassword() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword("weak")
                .confirmPassword("weak")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========================================================================
    // US-01003 & US-01004: Change Password
    // ========================================================================

    @Test
    @DisplayName("US-01003: PUT /api/v1/auth/change-password - Authenticated user changes password successfully")
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword@2026")
                .newPassword("NewCompliantPassword@2026")
                .confirmPassword("NewCompliantPassword@2026")
                .build();

        AuthResponse response = AuthResponse.builder()
                .userId("u-101")
                .username("teacher_vikram")
                .mustChangePassword(false)
                .message("Password changed successfully")
                .build();

        when(authService.changePassword(any(ChangePasswordRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mustChangePassword").value(false))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("US-01003: PUT /api/v1/auth/change-password - Missing authorization header returns 401 Unauthorized")
    void testChangePassword_Unauthenticated() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword@2026")
                .newPassword("NewCompliantPassword@2026")
                .confirmPassword("NewCompliantPassword@2026")
                .build();

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("US-01003: PUT /api/v1/auth/change-password - Incorrect current password throws 401 Unauthorized")
    void testChangePassword_IncorrectCurrentPassword() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword@123")
                .newPassword("NewCompliantPassword@2026")
                .confirmPassword("NewCompliantPassword@2026")
                .build();

        when(authService.changePassword(any(ChangePasswordRequest.class)))
                .thenThrow(new UnauthorizedException("Current password verification failed"));

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Current password verification failed"));
    }

    // ========================================================================
    // US-01004: First-time Login Flag
    // ========================================================================

    @Test
    @DisplayName("US-01004: POST /api/v1/auth/login - Must change password flag returns true in envelope")
    void testLogin_MustChangePasswordEnforced() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("new_teacher")
                .password("TempPassword@2026")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("temp-restricted-token")
                .userId("u-999")
                .username("new_teacher")
                .mustChangePassword(true)
                .roles(List.of("ROLE_PASSWORD_CHANGE_REQUIRED"))
                .message("Password change is required before accessing operational platform services")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_PASSWORD_CHANGE_REQUIRED"));
    }

    // ========================================================================
    // US-01005: Admin Account Unlock & Temporary Password Reset
    // ========================================================================

    @Test
    @DisplayName("US-01005: POST /api/v1/users/{userId}/unlock - Super Admin unlocks account successfully")
    void testUnlockUser_SuperAdminSuccess() throws Exception {
        UserUnlockResponse response = UserUnlockResponse.builder()
                .userId("u-locked-101")
                .username("teacher_vikram")
                .unlocked(true)
                .message("User account unlocked successfully")
                .unlockedAt(Instant.now())
                .build();

        when(authService.unlockUser("u-locked-101")).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/u-locked-101/unlock")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unlocked").value(true))
                .andExpect(jsonPath("$.data.userId").value("u-locked-101"));
    }

    @Test
    @DisplayName("US-01005: POST /api/v1/users/{userId}/unlock - Unauthorized role returns 403 Forbidden")
    void testUnlockUser_ForbiddenForTeacher() throws Exception {
        mockMvc.perform(post("/api/v1/users/u-locked-101/unlock")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-01005: POST /api/v1/users/{userId}/admin-reset-password - Principal resets password successfully")
    void testAdminResetPassword_PrincipalSuccess() throws Exception {
        AdminResetPasswordResponse response = AdminResetPasswordResponse.builder()
                .userId("u-target-102")
                .username("student_rahul")
                .temporaryPassword("Temp@1234abcd!9")
                .mustChangePassword(true)
                .message("Temporary password generated successfully")
                .resetAt(Instant.now())
                .build();

        when(authService.adminResetPassword("u-target-102")).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/u-target-102/admin-reset-password")
                        .header("Authorization", "Bearer " + principalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value("Temp@1234abcd!9"));
    }
}
