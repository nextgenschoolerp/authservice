package com.kvs.auth.controller;

import com.kvs.auth.dto.RegionalSettingsRequest;
import com.kvs.auth.dto.RegionalSettingsResponse;
import com.kvs.auth.service.RegionalSettingsService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Regional preferences of a tenant (Settings → General → Regional preferences): time zone, currency,
 * date format and language. Any signed-in user of the tenant reads them (the UI formats every screen with them);
 * school administrators save them. Callers of another tenant are refused unless they are SUPER_ADMIN.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tenants")
public class RegionalSettingsController {

    @Autowired
    private RegionalSettingsService regionalSettingsService;

    @GetMapping("/{tenantId}/regional-settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RegionalSettingsResponse>> getSettings(@PathVariable("tenantId") String tenantId) {
        log.info("Request to read regional settings for tenantId: {}", tenantId);
        return ResponseEntity.ok(ApiResponse.success(regionalSettingsService.getSettings(tenantId)));
    }

    @PutMapping("/{tenantId}/regional-settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TRUSTEE', 'SCHOOL_PRINCIPAL', 'PRINCIPAL', 'IT_SYSTEM_ADMIN')"
            + " or hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_TRUSTEE', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_PRINCIPAL', 'ROLE_IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<RegionalSettingsResponse>> saveSettings(
            @PathVariable("tenantId") String tenantId,
            @Valid @RequestBody RegionalSettingsRequest request) {
        log.info("Request to save regional settings for tenantId: {}", tenantId);
        RegionalSettingsResponse response = regionalSettingsService.saveSettings(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Regional preferences saved", response));
    }
}
