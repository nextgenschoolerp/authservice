package com.kvs.auth.service;

import com.kvs.auth.dto.*;
import com.kvs.auth.entity.*;
import com.kvs.auth.repo.*;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.exception.UnauthorizedException;
import com.kvs.core.service.JwtTokenService;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing Super Admin self-registration, tenant onboarding, authentication,
 * password verification, account lockout, session management, and forensic audit logging.
 */
@Service
@Slf4j
public class AuthService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    /**
     * Register a new educational society/trust and provision the root Super Administrator account.
     *
     * @param request registration payload containing tenant and Super Admin details
     * @return response with created tenant and Super Admin account info
     */
    @Transactional
    public SuperAdminRegisterResponse registerSuperAdmin(SuperAdminRegisterRequest request) {
        log.info("Registering Super Admin: {} for society: {}", request.getUsername(), request.getSocietyCode());

        // 1. Conflict validations
        if (tenantRepository.existsBySocietyCode(request.getSocietyCode())) {
            log.warn("Registration rejected: Society code {} already exists", request.getSocietyCode());
            throw new ConflictException("Society with code '" + request.getSocietyCode() + "' is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected: Username {} already exists", request.getUsername());
            throw new ConflictException("Username '" + request.getUsername() + "' is already in use");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected: Email {} already exists", request.getEmail());
            throw new ConflictException("Email '" + request.getEmail() + "' is already in use");
        }

        // 2. Create Tenant
        String tenantId = UUID.randomUUID().toString();
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setSocietyCode(request.getSocietyCode());
        tenant.setSocietyName(request.getSocietyName());
        tenant.setDomainName(request.getDomainName());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setStatus("ACTIVE");
        Tenant savedTenant = tenantRepository.save(tenant);

        // 3. Create Super Admin User
        String userId = UUID.randomUUID().toString();
        User user = new User();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setBranchId(request.getBranchCode() != null ? request.getBranchCode() : "MAIN-CAMPUS");
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setUserType("SUPER_ADMIN");
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setActive(true);
        user.setMfaEnabled(false);
        User savedUser = userRepository.save(user);

        // 4. Create User Credentials with BCrypt Hash
        UserCredential credential = new UserCredential();
        credential.setUserId(userId);
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        credential.setPasswordSalt(UUID.randomUUID().toString());
        credential.setPasswordChangedAt(Instant.now());
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePassword(false);
        userCredentialRepository.save(credential);

        // 5. Assign Root Super Admin Role
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleCode("ROLE_SUPER_ADMIN");
        userRoleRepository.save(userRole);

        // 6. Write Forensic Audit Log
        writeAuditLog(userId, "REGISTER_SUPER_ADMIN", "users", "SUCCESS",
                "Super Admin '" + user.getUsername() + "' onboarded successfully for society '" + tenant.getSocietyName() + "'");

        log.info("Super Admin registered successfully: userId={}, tenantId={}", userId, tenantId);

        return SuperAdminRegisterResponse.builder()
                .tenantId(tenantId)
                .societyCode(savedTenant.getSocietyCode())
                .societyName(savedTenant.getSocietyName())
                .userId(userId)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .userType("SUPER_ADMIN")
                .roles(List.of("ROLE_SUPER_ADMIN"))
                .status("ACTIVE")
                .message("Super Admin and Educational Society onboarded successfully")
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Authenticate Super Admin or other platform users using credentials.
     * Enforces account lockout rules (5 consecutive failed attempts lock for 30 minutes).
     *
     * @param request login request with username and password
     * @return AuthResponse with JWT token and user profile
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Authentication attempt for user: {}", request.getUsername());

        // 1. Lookup user by username or email
        User user = findUserByUsernameOrEmail(request.getUsername(), request.getTenantId());
        if (user == null) {
            writeAuditLog("anonymous", "LOGIN_FAILURE", "user_credentials", "FAILURE",
                    "Invalid login attempt for unknown user: " + request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }

        // 2. Check if user is active
        if (!user.isActive()) {
            writeAuditLog(user.getUserId(), "LOGIN_FAILURE", "users", "FAILURE",
                    "Login attempt on deactivated account: " + user.getUsername());
            throw new UnauthorizedException("User account is inactive. Please contact your administrator.");
        }

        // 3. Retrieve Credentials and evaluate Lockout Status
        UserCredential credential = userCredentialRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (credential.getFailedLoginAttempts() >= 5
                && credential.getLockedUntil() != null
                && credential.getLockedUntil().isAfter(Instant.now())) {
            writeAuditLog(user.getUserId(), "LOGIN_LOCKED", "user_credentials", "FAILURE",
                    "Login rejected. Account is locked until " + credential.getLockedUntil());
            throw new UnauthorizedException("Account is locked due to 5 consecutive failed login attempts. Locked until: "
                    + credential.getLockedUntil());
        }

        // 4. Verify Password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), credential.getPasswordHash());
        if (!passwordMatches) {
            int newAttempts = credential.getFailedLoginAttempts() + 1;
            credential.setFailedLoginAttempts(newAttempts);
            if (newAttempts >= 5) {
                Instant lockExpiry = Instant.now().plus(30, ChronoUnit.MINUTES);
                credential.setLockedUntil(lockExpiry);
                log.error("User {} locked out after {} failed attempts until {}", user.getUsername(), newAttempts, lockExpiry);
            }
            userCredentialRepository.save(credential);

            writeAuditLog(user.getUserId(), "LOGIN_FAILURE", "user_credentials", "FAILURE",
                    "Failed password attempt (" + newAttempts + "/5) for user: " + user.getUsername());

            if (newAttempts >= 5) {
                throw new UnauthorizedException("Account is locked due to 5 consecutive failed login attempts. Locked until: "
                        + credential.getLockedUntil());
            }
            throw new UnauthorizedException("Invalid username or password");
        }

        // 5. Successful password match - reset lockout counters
        if (credential.getFailedLoginAttempts() > 0 || credential.getLockedUntil() != null) {
            credential.setFailedLoginAttempts(0);
            credential.setLockedUntil(null);
            userCredentialRepository.save(credential);
        }

        // 6. Retrieve assigned user roles
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        List<String> roleCodes = userRoles.isEmpty()
                ? List.of("ROLE_" + user.getUserType())
                : userRoles.stream().map(UserRole::getRoleCode).collect(Collectors.toList());

        // 7. Establish User Session
        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession();
        session.setSessionId(sessionId);
        session.setUserId(user.getUserId());
        session.setTenantId(user.getTenantId());
        session.setSessionTokenHash(UUID.randomUUID().toString());
        session.setIpAddress(getClientIpAddress());
        session.setUserAgent(httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown");
        session.setDeviceFingerprint(request.getDeviceFingerprint());
        session.setLoginAt(Instant.now());
        session.setLastActivityAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        session.setRevoked(false);
        userSessionRepository.save(session);

        // 8. Generate JWT Token
        Map<String, Object> claims = new HashMap<>();
        boolean mustChangePassword = credential.isMustChangePassword();
        if (mustChangePassword) {
            claims.put("roles", List.of("ROLE_PASSWORD_CHANGE_REQUIRED"));
            claims.put("scope", "SCOPE_PASSWORD_CHANGE");
            claims.put("mustChangePassword", true);
        } else {
            claims.put("roles", roleCodes);
        }
        claims.put("userId", user.getUserId());
        // The subject is the username; services that match records by email (e.g. a parent's own applications) read this claim
        if (user.getEmail() != null) {
            claims.put("email", user.getEmail());
        }
        claims.put("tenantId", user.getTenantId());
        claims.put("userType", user.getUserType());
        claims.put("mustChangePassword", mustChangePassword);

        String jwtToken = jwtTokenService.generateToken(user.getUsername(), claims);

        // 9. Write Audit Log
        writeAuditLog(user.getUserId(), "LOGIN_SUCCESS", "user_sessions", "SUCCESS",
                "User '" + user.getUsername() + "' logged in successfully from IP: " + getClientIpAddress());

        log.info("User {} authenticated successfully with roles {}", user.getUsername(), roleCodes);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .branchId(user.getBranchId())
                .userType(user.getUserType())
                .roles(mustChangePassword ? List.of("ROLE_PASSWORD_CHANGE_REQUIRED") : roleCodes)
                .mfaRequired(user.isMfaEnabled())
                .sessionId(sessionId)
                .mustChangePassword(mustChangePassword)
                .message(mustChangePassword
                        ? "Password change is required before accessing operational platform services"
                        : "Login successful")
                .build();
    }

    /**
     * Retrieve the current authenticated user's profile and granted roles.
     *
     * @param username username of the authenticated principal
     * @return current user profile
     */
    public AuthResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<String> roles = userRoleRepository.findByUserId(user.getUserId()).stream()
                .map(UserRole::getRoleCode)
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            roles = List.of("ROLE_" + user.getUserType());
        }

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .branchId(user.getBranchId())
                .userType(user.getUserType())
                .roles(roles)
                .mfaRequired(user.isMfaEnabled())
                .message("User profile retrieved successfully")
                .build();
    }

    /**
     * Request a password reset token for an active user (US-01001).
     * Dispatches token and returns generic success envelope to prevent enumeration.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password requested for identifier: {}", request.getIdentifier());

        User user = findUserByUsernameOrEmail(request.getIdentifier(), request.getTenantId());
        if (user == null || !user.isActive()) {
            writeAuditLog("anonymous", "PASSWORD_RESET_REJECTED", "user_credentials", "FAILURE",
                    "Password reset requested for non-existent or inactive user: " + request.getIdentifier());
            return ForgotPasswordResponse.builder()
                    .message("If an account matches your details, a reset notification has been sent")
                    .build();
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiry = Instant.now().plus(15, ChronoUnit.MINUTES);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(rawToken);
        resetToken.setUserId(user.getUserId());
        resetToken.setExpiresAt(expiry);
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        writeAuditLog(user.getUserId(), "PASSWORD_RESET_REQUESTED", "user_credentials", "SUCCESS",
                "Password reset token generated for user: " + user.getUsername());

        return ForgotPasswordResponse.builder()
                .message("If an account matches your details, a reset notification has been sent")
                .resetToken(rawToken)
                .expiresAt(expiry)
                .build();
    }

    /**
     * Complete password reset using valid, unexpired reset token (US-01002).
     */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password with verification token");

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(request.getToken())
                .orElseThrow(() -> {
                    writeAuditLog("anonymous", "PASSWORD_RESET_TOKEN_INVALID", "password_reset_tokens", "FAILURE",
                            "Invalid or already used password reset token");
                    return new BadRequestException("Invalid or expired password reset token");
                });

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            writeAuditLog(resetToken.getUserId(), "PASSWORD_RESET_TOKEN_EXPIRED", "password_reset_tokens", "FAILURE",
                    "Expired password reset token used");
            throw new BadRequestException("Invalid or expired password reset token");
        }

        UserCredential credential = userCredentialRepository.findByUserId(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("UserCredential", "userId", resetToken.getUserId()));

        credential.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        credential.setPasswordChangedAt(Instant.now());
        credential.setMustChangePassword(false);
        userCredentialRepository.save(credential);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        revokeUserSessions(resetToken.getUserId());

        writeAuditLog(resetToken.getUserId(), "PASSWORD_RESET_SUCCESS", "user_credentials", "SUCCESS",
                "Password reset successfully completed via token");

        User user = userRepository.findByUserId(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", resetToken.getUserId()));

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .branchId(user.getBranchId())
                .userType(user.getUserType())
                .message("Password has been reset successfully")
                .build();
    }

    /**
     * Authenticated user password change with history verification (US-01003 & US-01004).
     */
    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        String username = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
        log.info("Password change requested by user: {}", username);

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        UserCredential credential = userCredentialRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("UserCredential", "userId", user.getUserId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), credential.getPasswordHash())) {
            writeAuditLog(user.getUserId(), "PASSWORD_CHANGE_FAILURE", "user_credentials", "FAILURE",
                    "Current password verification failed for user: " + username);
            throw new UnauthorizedException("Current password verification failed");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), credential.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as your current password");
        }

        boolean wasForced = credential.isMustChangePassword();
        credential.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        credential.setPasswordChangedAt(Instant.now());
        credential.setMustChangePassword(false);
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        userCredentialRepository.save(credential);

        String action = wasForced ? "FORCED_PASSWORD_CHANGE_COMPLETED" : "PASSWORD_CHANGE_SUCCESS";
        writeAuditLog(user.getUserId(), action, "user_credentials", "SUCCESS",
                "Password successfully updated for user: " + username);

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .branchId(user.getBranchId())
                .userType(user.getUserType())
                .mustChangePassword(false)
                .message("Password changed successfully")
                .build();
    }

    /**
     * Unlock a locked user account (US-01005).
     */
    @Transactional
    public UserUnlockResponse unlockUser(String targetUserId) {
        log.info("Unlocking user account for targetUserId: {}", targetUserId);

        User targetUser = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", targetUserId));
        UserCredential credential = userCredentialRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCredential", "userId", targetUserId));

        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        userCredentialRepository.save(credential);

        String caller = SecurityUtils.getCurrentUserEmail().orElse("admin");
        writeAuditLog(targetUserId, "ADMIN_ACCOUNT_UNLOCKED", "user_credentials", "SUCCESS",
                "Account unlocked by administrator: " + caller);

        return UserUnlockResponse.builder()
                .userId(targetUserId)
                .username(targetUser.getUsername())
                .unlocked(true)
                .message("User account unlocked successfully")
                .unlockedAt(Instant.now())
                .build();
    }

    /**
     * Administrative temporary credential reset with forced first-login reset flag (US-01005).
     */
    @Transactional
    public AdminResetPasswordResponse adminResetPassword(String targetUserId) {
        log.info("Administrative password reset requested for targetUserId: {}", targetUserId);

        User targetUser = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", targetUserId));
        UserCredential credential = userCredentialRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserCredential", "userId", targetUserId));

        String tempPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 8) + "!9";
        credential.setPasswordHash(passwordEncoder.encode(tempPassword));
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePassword(true);
        credential.setPasswordChangedAt(Instant.now());
        userCredentialRepository.save(credential);

        revokeUserSessions(targetUserId);

        String caller = SecurityUtils.getCurrentUserEmail().orElse("admin");
        writeAuditLog(targetUserId, "ADMIN_PASSWORD_RESET", "user_credentials", "SUCCESS",
                "Temporary password generated and must_change_password enforced by administrator: " + caller);

        return AdminResetPasswordResponse.builder()
                .userId(targetUserId)
                .username(targetUser.getUsername())
                .temporaryPassword(tempPassword)
                .mustChangePassword(true)
                .message("Temporary password generated successfully")
                .resetAt(Instant.now())
                .build();
    }

    @Transactional
    public RevokeSessionsResponse revokeUserSessionsWithDetails(String userId) {
        log.info("Immediate session revocation requested for employee/user: {}", userId);
        if (!userRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }

        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndRevokedFalse(userId);
        for (UserSession s : activeSessions) {
            s.setRevoked(true);
        }
        userSessionRepository.saveAll(activeSessions);

        String caller = SecurityUtils.getCurrentUserEmail().orElse("admin");
        writeAuditLog(userId, "USER_SESSIONS_REVOKED", "user_sessions", "SUCCESS",
                "Revoked " + activeSessions.size() + " active sessions by admin/supervisor: " + caller);

        return RevokeSessionsResponse.builder()
                .userId(userId)
                .revokedSessionsCount(activeSessions.size())
                .revokedAt(Instant.now())
                .message("All active user sessions revoked successfully")
                .build();
    }

    public List<UserSessionResponse> getUserSessions(String userId) {
        log.info("Fetching sessions for userId: {}", userId);
        if (!userRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }

        return userSessionRepository.findByUserId(userId).stream()
                .map(s -> UserSessionResponse.builder()
                        .sessionId(s.getSessionId())
                        .userId(s.getUserId())
                        .tenantId(s.getTenantId())
                        .ipAddress(s.getIpAddress())
                        .userAgent(s.getUserAgent())
                        .loginAt(s.getLoginAt())
                        .lastActivityAt(s.getLastActivityAt())
                        .expiresAt(s.getExpiresAt())
                        .revoked(s.isRevoked())
                        .build())
                .toList();
    }

    private void revokeUserSessions(String userId) {
        try {
            List<UserSession> activeSessions = userSessionRepository.findByUserIdAndRevokedFalse(userId);
            for (UserSession session : activeSessions) {
                session.setRevoked(true);
            }
            userSessionRepository.saveAll(activeSessions);
        } catch (Exception e) {
            log.warn("Failed to revoke active sessions for user {}: {}", userId, e.getMessage());
        }
    }

    private User findUserByUsernameOrEmail(String identifier, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return userRepository.findByTenantIdAndUsername(tenantId, identifier)
                    .or(() -> userRepository.findByTenantIdAndEmail(tenantId, identifier))
                    .orElse(null);
        }
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);
    }

    private void writeAuditLog(String userId, String action, String resource, String outcome, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setIpAddress(getClientIpAddress());
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setOutcome(outcome);
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log entry: {}", e.getMessage());
        }
    }

    private String getClientIpAddress() {
        if (httpRequest != null && httpRequest.getRemoteAddr() != null) {
            String forwarded = httpRequest.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return httpRequest.getRemoteAddr();
        }
        return "127.0.0.1";
    }
}

