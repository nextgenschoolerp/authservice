package com.kvs.auth.controller;

import com.kvs.auth.dto.SchoolProfileRequest;
import com.kvs.auth.dto.SchoolProfileResponse;
import com.kvs.auth.entity.TenantSchoolProfile;
import com.kvs.auth.service.SchoolProfileService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * School profile and crest (Settings → General → School profile). Any signed-in user of the tenant reads them;
 * the same administrators who save regional preferences save them.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tenants")
public class SchoolProfileController {

    private static final String EDITORS = "hasAnyRole('SUPER_ADMIN', 'TRUSTEE', 'SCHOOL_PRINCIPAL', 'PRINCIPAL', 'IT_SYSTEM_ADMIN')"
            + " or hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_TRUSTEE', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_PRINCIPAL', 'ROLE_IT_SYSTEM_ADMIN')";

    @Autowired
    private SchoolProfileService profileService;

    @GetMapping("/{tenantId}/school-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SchoolProfileResponse>> getProfile(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile(tenantId)));
    }

    @PutMapping("/{tenantId}/school-profile")
    @PreAuthorize(EDITORS)
    public ResponseEntity<ApiResponse<SchoolProfileResponse>> saveProfile(@PathVariable("tenantId") String tenantId,
                                                                          @Valid @RequestBody SchoolProfileRequest request) {
        log.info("Request to save the school profile for tenantId: {}", tenantId);
        return ResponseEntity.ok(ApiResponse.success("School profile saved", profileService.saveProfile(tenantId, request)));
    }

    @PutMapping(value = "/{tenantId}/school-profile/crest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(EDITORS)
    public ResponseEntity<ApiResponse<SchoolProfileResponse>> saveCrest(@PathVariable("tenantId") String tenantId,
                                                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("School crest updated", profileService.saveCrest(tenantId, file)));
    }

    @GetMapping("/{tenantId}/school-profile/crest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getCrest(@PathVariable("tenantId") String tenantId) {
        TenantSchoolProfile profile = profileService.crest(tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(profile.getCrestContentType()))
                .cacheControl(CacheControl.noCache())
                .body(profile.getCrest());
    }
}
