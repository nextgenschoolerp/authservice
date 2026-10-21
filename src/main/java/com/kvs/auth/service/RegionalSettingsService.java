package com.kvs.auth.service;

import com.kvs.auth.dto.RegionalSettingsRequest;
import com.kvs.auth.dto.RegionalSettingsResponse;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.TenantRegionalSettings;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.TenantRegionalSettingsRepository;
import com.kvs.auth.repo.TenantRepository;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Per-tenant regional preferences (time zone, currency, date format, language) shown and edited in
 * Settings → General → Regional preferences. Every signed-in user of a tenant reads them; administrators save them.
 * A tenant that never saved any gets the defaults.
 */
@Service
@Slf4j
public class RegionalSettingsService {

    static final String DEFAULT_TIME_ZONE = "Asia/Kolkata";
    static final String DEFAULT_CURRENCY = "INR";
    static final String DEFAULT_DATE_FORMAT = "DD MMM YYYY";
    static final String DEFAULT_LANGUAGE = "en-IN";

    @Autowired
    private TenantRegionalSettingsRepository settingsRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TenantAccessGuard tenantGuard;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    /**
     * The tenant's regional preferences, or the defaults when none are saved.
     *
     * @throws com.kvs.core.exception.ForbiddenException when the caller belongs to another tenant
     * @throws ResourceNotFoundException                 when the tenant does not exist
     */
    public RegionalSettingsResponse getSettings(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        requireTenantExists(tenantId);
        return settingsRepository.findByTenantId(tenantId)
                .map(this::mapToResponse)
                .orElseGet(() -> RegionalSettingsResponse.builder()
                        .tenantId(tenantId)
                        .timeZone(DEFAULT_TIME_ZONE)
                        .currency(DEFAULT_CURRENCY)
                        .dateFormat(DEFAULT_DATE_FORMAT)
                        .language(DEFAULT_LANGUAGE)
                        .saved(false)
                        .build());
    }

    /**
     * Saves the tenant's regional preferences and records an audit entry.
     *
     * @throws com.kvs.core.exception.ForbiddenException when the caller belongs to another tenant
     * @throws ResourceNotFoundException                 when the tenant does not exist
     */
    @Transactional
    public RegionalSettingsResponse saveSettings(String tenantId, RegionalSettingsRequest request) {
        tenantGuard.requireTenant(tenantId);
        requireTenantExists(tenantId);

        TenantRegionalSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantRegionalSettings created = new TenantRegionalSettings();
                    created.setTenantId(tenantId);
                    return created;
                });
        settings.setTimeZone(request.getTimeZone());
        settings.setCurrency(request.getCurrency());
        settings.setDateFormat(request.getDateFormat());
        settings.setLanguage(request.getLanguage());
        TenantRegionalSettings saved = settingsRepository.save(settings);
        log.info("Saved regional settings for tenantId: {}", tenantId);

        logAudit(tenantId, "Saved regional preferences: timeZone=" + request.getTimeZone()
                + ", currency=" + request.getCurrency() + ", dateFormat=" + request.getDateFormat()
                + ", language=" + request.getLanguage());
        return mapToResponse(saved);
    }

    private void requireTenantExists(String tenantId) {
        if (tenantRepository.findByTenantId(tenantId).isEmpty()) {
            throw new ResourceNotFoundException("Tenant", "tenantId", tenantId);
        }
    }

    private void logAudit(String tenantId, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(SecurityUtils.getCurrentUserEmail().orElse("unknown"));
            auditLog.setIpAddress(httpRequest != null && httpRequest.getRemoteAddr() != null ? httpRequest.getRemoteAddr() : "127.0.0.1");
            auditLog.setAction("UPDATE_REGIONAL_SETTINGS");
            auditLog.setResource("tenant:" + tenantId);
            auditLog.setOutcome("SUCCESS");
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit log for tenantId {}: {}", tenantId, e.getMessage());
        }
    }

    private RegionalSettingsResponse mapToResponse(TenantRegionalSettings entity) {
        return RegionalSettingsResponse.builder()
                .tenantId(entity.getTenantId())
                .timeZone(entity.getTimeZone())
                .currency(entity.getCurrency())
                .dateFormat(entity.getDateFormat())
                .language(entity.getLanguage())
                .saved(true)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy() : entity.getCreatedBy())
                .build();
    }
}
