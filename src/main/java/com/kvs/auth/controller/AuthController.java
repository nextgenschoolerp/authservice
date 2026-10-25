package com.kvs.auth.controller;

import com.kvs.auth.dto.*;
import com.kvs.auth.service.AuthService;
import com.kvs.core.dto.ApiResponse;
import com.kvs.core.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.kvs.auth.service.BootstrapService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller providing public and authenticated identity endpoints:
 * 1. Super Admin Self-Registration and Educational Society Onboarding
 * 2. Multi-tenant User Authentication (Login) with Lockout Protection
 * 3. Authenticated Principal Profile Inspection (/me)
 * 4. Password Lifecycle Management (Forgot, Reset, Change, Admin Reset, Unlock)
 * 5. System Bootstrap and Initial Onboarding from Excel or Direct Payload
 */
@RestController
@Slf4j
@RequestMapping({"/api/v1/auth", "/api/v1/users"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private BootstrapService bootstrapService;

    /**
     * Inspect if the system requires initial setup/bootstrap.
     */
    @GetMapping("/bootstrap/status")
    public ResponseEntity<ApiResponse<BootstrapStatusResponse>> getBootstrapStatus() {
        log.info("Request to check system bootstrap status");
        return ResponseEntity.ok(ApiResponse.success(bootstrapService.getBootstrapStatus()));
    }

    /**
     * Download the standard Excel onboarding template.
     */
    @GetMapping("/bootstrap/template")
    public ResponseEntity<byte[]> downloadBootstrapTemplate() throws Exception {
        log.info("Request to download Excel bootstrap template");
        byte[] bytes = bootstrapService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"School_ERP_Initial_Setup_Template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Perform initial system bootstrap via JSON payload.
     */
    @PostMapping(value = "/bootstrap", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<BootstrapResponse>> bootstrapJson(
            @Valid @RequestBody BootstrapRequest request) {
        log.info("Request for JSON-based system bootstrap: {}", request.getSocietyCode());
        BootstrapResponse response = bootstrapService.bootstrap(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("System initialized successfully", response));
    }

    /**
     * Perform initial system bootstrap via uploaded Excel file.
     */
    @PostMapping(value = "/bootstrap/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BootstrapResponse>> bootstrapUpload(
            @RequestParam("file") MultipartFile file) throws Exception {
        log.info("Request for Excel upload system bootstrap: {}", file.getOriginalFilename());
        BootstrapResponse response = bootstrapService.bootstrapFromUpload(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("System initialized successfully from Excel", response));
    }

    /**
     * Register a new educational trust/society and provision the root Super Administrator account.
     *
     * @param request the registration details
     * @return 201 Created envelope with created tenant and Super Admin details
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SuperAdminRegisterResponse>> registerSuperAdmin(
            @Valid @RequestBody SuperAdminRegisterRequest request) {
        log.info("Request to register Super Admin: {} for society: {}", request.getUsername(), request.getSocietyCode());
        SuperAdminRegisterResponse response = authService.registerSuperAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Super Administrator and Educational Society onboarded successfully", response));
    }

    /**
     * Authenticate Super Admin or platform user with credentials.
     * Enforces account lockout rules (5 consecutive failed attempts lock for 30 minutes).
     *
     * @param request username/email, password, and optional tenant context
     * @return 200 OK envelope with JWT access token and user identity
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Request to authenticate user: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * Retrieve the currently authenticated principal's profile details.
     *
     * @return 200 OK envelope with authenticated user profile
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser() {
        String username = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new IllegalStateException("No authenticated principal found in security context"));
        log.info("Request to retrieve authenticated profile for: {}", username);
        AuthResponse response = authService.getCurrentUserProfile(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Renews the signed-in user's token while they are active (sliding session). The current bearer token must still be valid.
     *
     * @param authorization the bearer token of the session being renewed
     * @return 200 OK envelope with a fresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        return ResponseEntity.ok(ApiResponse.success(authService.refreshAccessToken(token)));
    }

    /**
     * Request a time-limited single-use password reset token (US-01001).
     *
     * @param request identifier (username or email)
     * @return 200 OK generic envelope to prevent user enumeration
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Request for password reset token for: {}", request.getIdentifier());
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    /**
     * Reset account password using single-use verification token (US-01002).
     *
     * @param request token and new compliant password
     * @return 200 OK envelope confirming password reset
     */
    @PostMapping({"/reset-password", "/password-reset"})
    public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Request to reset password with token");
        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", response));
    }

    /**
     * Authenticated user self-service password change (US-01003 and US-01004).
     *
     * @param request current password, new compliant password, confirmation
     * @return 200 OK envelope confirming password change
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Authenticated request to change password");
        AuthResponse response = authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
    }

    /**
     * Administrative account unlock (US-01005).
     *
     * @param userId unique target user identifier
     * @return 200 OK envelope confirming account unlock
     */
    @PostMapping({"/{userId}/unlock", "/unlock/{userId}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRINCIPAL')")
    public ResponseEntity<ApiResponse<UserUnlockResponse>> unlockUser(
            @PathVariable("userId") String userId) {
        log.info("Administrative unlock request for userId: {}", userId);
        UserUnlockResponse response = authService.unlockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User account unlocked successfully", response));
    }

    /**
     * Administrative temporary password reset and forced reset flag enforcement (US-01005).
     *
     * @param userId unique target user identifier
     * @return 200 OK envelope with generated temporary password
     */
    @PostMapping({"/{userId}/admin-reset-password", "/admin-reset-password/{userId}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRINCIPAL')")
    public ResponseEntity<ApiResponse<AdminResetPasswordResponse>> adminResetPassword(
            @PathVariable("userId") String userId) {
        log.info("Administrative password reset request for userId: {}", userId);
        AdminResetPasswordResponse response = authService.adminResetPassword(userId);
        return ResponseEntity.ok(ApiResponse.success("Temporary password generated successfully", response));
    }

    /**
     * Revoke active user sessions instantly upon employee resignation or security concern (US-00014).
     *
     * @param userId unique target user identifier
     * @return 200 OK envelope confirming session revocation count
     */
    @PostMapping({"/{userId}/sessions/revoke", "/sessions/revoke/{userId}"})
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('SUPER_ADMIN') or hasRole('PRINCIPAL') or hasRole('IT_SYSTEM_ADMIN') or hasAuthority('ROLE_DEPARTMENT_HEAD') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<com.kvs.auth.dto.RevokeSessionsResponse>> revokeUserSessions(
            @PathVariable("userId") String userId) {
        log.info("Request to revoke active sessions for userId: {}", userId);
        com.kvs.auth.dto.RevokeSessionsResponse response = authService.revokeUserSessionsWithDetails(userId);
        return ResponseEntity.ok(ApiResponse.success("All active user sessions revoked successfully", response));
    }

    /**
     * Retrieve active or historic user sessions for investigation (US-00014).
     *
     * @param userId unique target user identifier
     * @return 200 OK envelope with user sessions
     */
    @GetMapping({"/{userId}/sessions", "/sessions/{userId}"})
    @PreAuthorize("hasRole('DEPARTMENT_HEAD') or hasRole('SUPER_ADMIN') or hasRole('PRINCIPAL') or hasRole('IT_SYSTEM_ADMIN') or hasAuthority('ROLE_DEPARTMENT_HEAD') or hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_PRINCIPAL') or hasAuthority('ROLE_IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<com.kvs.auth.dto.UserSessionResponse>>> getUserSessions(
            @PathVariable("userId") String userId) {
        log.info("Request to inspect sessions for userId: {}", userId);
        java.util.List<com.kvs.auth.dto.UserSessionResponse> response = authService.getUserSessions(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
