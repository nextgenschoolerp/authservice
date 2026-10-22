package com.kvs.auth.service;

import com.kvs.auth.dto.SchoolProfileRequest;
import com.kvs.auth.dto.SchoolProfileResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.Tenant;
import com.kvs.auth.entity.TenantSchoolProfile;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.TenantRepository;
import com.kvs.auth.repo.TenantSchoolProfileRepository;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * The school profile and crest (Settings → General → School profile). Every signed-in user of a tenant reads it;
 * school administrators save it. A tenant that never saved one gets its name and contact details from the tenant record.
 */
@Service
@Slf4j
public class SchoolProfileService {

    static final Set<String> CREST_TYPES = Set.of("image/png", "image/jpeg", "image/svg+xml");
    static final long MAX_CREST_BYTES = 1024 * 1024;

    @Autowired
    private TenantSchoolProfileRepository profileRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TenantAccessGuard tenantGuard;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    public SchoolProfileResponse getProfile(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        Tenant tenant = tenant(tenantId);
        return profileRepository.findByTenantId(tenantId).map(this::map)
                .orElseGet(() -> SchoolProfileResponse.builder()
                        .tenantId(tenantId)
                        .schoolName(tenant.getSocietyName())
                        .campusName("Main campus")
                        .schoolEmail(tenant.getContactEmail())
                        .schoolPhone(tenant.getContactPhone())
                        .schoolReference(tenant.getSocietyCode())
                        .website(tenant.getDomainName())
                        .address("")
                        .saved(false)
                        .build());
    }

    @Transactional
    public SchoolProfileResponse saveProfile(String tenantId, SchoolProfileRequest request) {
        tenantGuard.requireTenant(tenantId);
        tenant(tenantId);
        TenantSchoolProfile profile = profileRepository.findByTenantId(tenantId).orElseGet(() -> {
            TenantSchoolProfile created = new TenantSchoolProfile();
            created.setTenantId(tenantId);
            return created;
        });
        profile.setSchoolName(request.getSchoolName().trim());
        profile.setCampusName(request.getCampusName().trim());
        profile.setSchoolEmail(request.getSchoolEmail().trim());
        profile.setSchoolPhone(request.getSchoolPhone().trim());
        profile.setSchoolReference(blankToNull(request.getSchoolReference()));
        profile.setWebsite(blankToNull(request.getWebsite()));
        profile.setAddress(request.getAddress().trim());
        TenantSchoolProfile saved = profileRepository.save(profile);
        logAudit(tenantId, "UPDATE_SCHOOL_PROFILE", "Saved school profile: " + saved.getSchoolName());
        return map(saved);
    }

    /** Replaces the crest (PNG, JPEG or SVG, at most 1 MB); the profile must have been saved first. */
    @Transactional
    public SchoolProfileResponse saveCrest(String tenantId, MultipartFile file) {
        tenantGuard.requireTenant(tenantId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image for the crest");
        }
        if (!CREST_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("The crest must be a PNG, JPEG or SVG image");
        }
        if (file.getSize() > MAX_CREST_BYTES) {
            throw new BadRequestException("The crest must be 1 MB or smaller");
        }
        TenantSchoolProfile profile = profileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BadRequestException("Save the school profile before adding a crest"));
        try {
            profile.setCrest(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("The image could not be read");
        }
        profile.setCrestContentType(file.getContentType());
        logAudit(tenantId, "UPDATE_SCHOOL_CREST", "Replaced school crest (" + file.getSize() + " bytes)");
        return map(profileRepository.save(profile));
    }

    public TenantSchoolProfile crest(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        return profileRepository.findByTenantId(tenantId)
                .filter(p -> p.getCrest() != null)
                .orElseThrow(() -> new ResourceNotFoundException("No crest has been uploaded for this school"));
    }

    private Tenant tenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "tenantId", tenantId));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void logAudit(String tenantId, String action, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(SecurityUtils.getCurrentUserEmail().orElse("unknown"));
            auditLog.setIpAddress(httpRequest != null && httpRequest.getRemoteAddr() != null ? httpRequest.getRemoteAddr() : "127.0.0.1");
            auditLog.setAction(action);
            auditLog.setResource("tenant:" + tenantId);
            auditLog.setOutcome("SUCCESS");
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit log for tenantId {}: {}", tenantId, e.getMessage());
        }
    }

    private SchoolProfileResponse map(TenantSchoolProfile p) {
        return SchoolProfileResponse.builder()
                .tenantId(p.getTenantId())
                .schoolName(p.getSchoolName())
                .campusName(p.getCampusName())
                .schoolEmail(p.getSchoolEmail())
                .schoolPhone(p.getSchoolPhone())
                .schoolReference(p.getSchoolReference())
                .website(p.getWebsite())
                .address(p.getAddress())
                .hasCrest(p.getCrest() != null)
                .saved(true)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                .build();
    }
}
