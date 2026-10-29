package com.kvs.auth.service;

import com.kvs.auth.dto.StaffAccountRequest;
import com.kvs.auth.dto.StaffAccountResult;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.Role;
import com.kvs.auth.entity.User;
import com.kvs.auth.entity.UserCredential;
import com.kvs.auth.entity.UserRole;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.RoleRepository;
import com.kvs.auth.repo.UserCredentialRepository;
import com.kvs.auth.repo.UserRepository;
import com.kvs.auth.repo.UserRoleRepository;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Creates sign-in accounts for staff added by the bulk import (Settings → Data import → Create sign-in accounts).
 * Each account gets a one-time temporary password that must be changed at first sign-in. The password is returned once
 * and never stored in clear or written to the audit log. Roles that carry school-wide or security authority cannot be
 * given from here (SUPER_ADMIN, TRUSTEE, parents never; principals, IT and auditors only by a super administrator).
 */
@Service
@Slf4j
public class StaffAccountService {

    private static final Set<String> NEVER = Set.of("SUPER_ADMIN", "TRUSTEE", "APPLICANT_PARENT");
    private static final Set<String> SUPER_ADMIN_ONLY = Set.of("PRINCIPAL", "SCHOOL_PRINCIPAL", "VICE_PRINCIPAL", "IT_SYSTEM_ADMIN", "SECURITY_AUDITOR", "AUDITOR");

    @Autowired private UserRepository userRepository;
    @Autowired private UserCredentialRepository credentialRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TenantAccessGuard tenantGuard;
    @Autowired private PlatformTransactionManager transactionManager;

    public List<StaffAccountResult> create(StaffAccountRequest request) {
        String tenantId = tenantGuard.currentTenantId().orElseThrow(() -> new BadRequestException("The school could not be identified from your sign-in"));
        boolean superAdmin = tenantGuard.isSuperAdmin();
        Map<String, Role> roles = new HashMap<>();
        roleRepository.findAll().stream().filter(Role::isActive).forEach(r -> roles.put(key(r.getName()), r));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return request.accounts().stream().map(a -> {
            try {
                return tx.execute(s -> createOne(tenantId, superAdmin, roles, a));
            } catch (RuntimeException e) {
                log.warn("Account for {} failed: {}", a.employeeCode(), e.getMessage());
                return failed(a, "Could not be created: " + e.getMessage());
            }
        }).toList();
    }

    private StaffAccountResult createOne(String tenantId, boolean superAdmin, Map<String, Role> roles, StaffAccountRequest.Account a) {
        if (blank(a.firstName()) || blank(a.lastName())) return failed(a, "First and last name are required");
        String email = a.email() == null ? "" : a.email().trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$")) return failed(a, "The email address is not valid");
        if (blank(a.branchId())) return failed(a, "The campus is missing");
        Role role = blank(a.roleName()) ? null : roles.get(key(a.roleName()));
        if (role == null) return failed(a, blank(a.roleName()) ? "No System role was given in the file" : "\"" + a.roleName() + "\" is not a role of this school");
        String code = role.getName();
        if (NEVER.contains(code) || (SUPER_ADMIN_ONLY.contains(code) && !superAdmin)) return failed(a, "The " + a.roleName() + " role cannot be given from an import");
        if (userRepository.existsByEmail(email)) {
            String owner = userRepository.findByEmail(email).map(User::getUsername).orElse("another user");
            return failed(a, email + " already has a sign-in account (" + owner + ")");
        }

        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 3) base = (a.firstName() + "." + a.lastName()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        String username = base;
        for (int n = 2; userRepository.existsByUsername(username); n++) username = base + n;

        String userId = UUID.randomUUID().toString();
        User user = new User();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setBranchId(a.branchId());
        user.setUsername(username);
        user.setEmail(email);
        user.setUserType(code);
        user.setFirstName(a.firstName().trim());
        user.setLastName(a.lastName().trim());
        user.setActive(true);
        user.setMfaEnabled(false);
        userRepository.save(user);

        String temp = "Temp@" + UUID.randomUUID().toString().substring(0, 8) + "!9";
        UserCredential credential = new UserCredential();
        credential.setUserId(userId);
        credential.setPasswordHash(passwordEncoder.encode(temp));
        credential.setPasswordSalt(UUID.randomUUID().toString());
        credential.setPasswordChangedAt(Instant.now());
        credential.setMustChangePassword(true);
        credentialRepository.save(credential);

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleCode(code);
        userRoleRepository.save(userRole);

        audit(userId, "Created sign-in account " + username + " (" + code + ") for employee " + a.employeeCode() + "; first sign-in requires a new password");
        return new StaffAccountResult(a.employeeCode(), "CREATED", userId, username, code, temp, "Account created; the temporary password must be changed at first sign-in");
    }

    private static StaffAccountResult failed(StaffAccountRequest.Account a, String message) {
        return new StaffAccountResult(a.employeeCode(), "FAILED", null, null, null, null, message);
    }

    private static String key(String s) { return s.replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT); }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private void audit(String userId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(SecurityUtils.getCurrentUserEmail().orElse("admin"));
            entry.setIpAddress("127.0.0.1");
            entry.setAction("CREATE_STAFF_ACCOUNT");
            entry.setResource("user:" + userId);
            entry.setOutcome("SUCCESS");
            entry.setEventTime(Instant.now());
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
