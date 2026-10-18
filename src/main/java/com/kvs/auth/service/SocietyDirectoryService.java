package com.kvs.auth.service;

import com.kvs.auth.dto.SocietyDirectoryResponse;
import com.kvs.auth.dto.StaffMemberResponse;
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
import com.kvs.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only directory of societies, campuses and staff (with roles and account state) for administrators,
 * e.g. to check that a school has staff holding the roles a workflow needs. Credential data is never returned.
 */
@Service
@Slf4j
public class SocietyDirectoryService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchProvisioningRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Transactional(readOnly = true)
    public List<SocietyDirectoryResponse> listSocieties() {
        Map<String, List<BranchProvisioning>> branches = branchRepository.findAll().stream()
                .collect(Collectors.groupingBy(BranchProvisioning::getTenantId));
        return tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getSocietyName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(t -> SocietyDirectoryResponse.builder()
                        .tenantId(t.getTenantId())
                        .societyCode(t.getSocietyCode())
                        .societyName(t.getSocietyName())
                        .domainName(t.getDomainName())
                        .status(t.getStatus())
                        .campuses(branches.getOrDefault(t.getTenantId(), List.of()).stream()
                                .sorted(Comparator.comparing(BranchProvisioning::getBranchName, Comparator.nullsLast(String::compareToIgnoreCase)))
                                .map(b -> SocietyDirectoryResponse.Campus.builder()
                                        .branchCode(b.getBranchCode())
                                        .branchName(b.getBranchName())
                                        .affiliationBoard(b.getAffiliationBoard())
                                        .status(b.getStatus())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    /**
     * Users of a society, optionally of one campus, with their roles and whether they can work
     * (active, not locked, no forced password change).
     */
    @Transactional(readOnly = true)
    public List<StaffMemberResponse> listStaff(String tenantId, String branchId) {
        tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "tenantId", tenantId));
        List<User> users = userRepository.findByTenantIdOrderByUsernameAsc(tenantId).stream()
                .filter(u -> branchId == null || branchId.isBlank() || branchId.equals(u.getBranchId()))
                .toList();
        Set<String> ids = users.stream().map(User::getUserId).collect(Collectors.toSet());
        Map<String, List<String>> roles = ids.isEmpty() ? Map.of() : userRoleRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.groupingBy(UserRole::getUserId, Collectors.mapping(UserRole::getRoleCode, Collectors.toList())));
        Map<String, UserCredential> credentials = ids.isEmpty() ? Map.of() : credentialRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(UserCredential::getUserId, Function.identity(), (a, b) -> a));
        Instant now = Instant.now();
        log.info("Directory: {} staff for tenant {} branch {}", users.size(), tenantId, branchId);
        return users.stream().map(u -> {
            UserCredential c = credentials.get(u.getUserId());
            Set<String> codes = new TreeSet<>();
            roles.getOrDefault(u.getUserId(), List.of()).forEach(r -> codes.add(r.replaceFirst("^ROLE_", "")));
            if (u.getUserType() != null) {
                codes.add(u.getUserType());
            }
            return StaffMemberResponse.builder()
                    .userId(u.getUserId())
                    .username(u.getUsername())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .email(u.getEmail())
                    .branchId(u.getBranchId())
                    .userType(u.getUserType())
                    .roles(List.copyOf(codes))
                    .active(u.isActive())
                    .locked(c != null && c.getLockedUntil() != null && c.getLockedUntil().isAfter(now))
                    .lockedUntil(c != null && c.getLockedUntil() != null && c.getLockedUntil().isAfter(now) ? c.getLockedUntil() : null)
                    .mustChangePassword(c != null && c.isMustChangePassword())
                    .build();
        }).toList();
    }
}
