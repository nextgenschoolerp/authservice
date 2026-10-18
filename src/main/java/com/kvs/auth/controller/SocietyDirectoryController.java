package com.kvs.auth.controller;

import com.kvs.auth.dto.SocietyDirectoryResponse;
import com.kvs.auth.dto.StaffMemberResponse;
import com.kvs.auth.service.SocietyDirectoryService;
import com.kvs.core.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only directory of societies, campuses and staff with their roles.
 * Used by administrators (and the admission workflow checker) to see who can act in which role.
 * Actor: Super Admin
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tenants")
public class SocietyDirectoryController {

    @Autowired
    private SocietyDirectoryService directoryService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SocietyDirectoryResponse>>> listSocieties() {
        return ResponseEntity.ok(ApiResponse.success(directoryService.listSocieties()));
    }

    @GetMapping("/{tenantId}/users")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffMemberResponse>>> listStaff(
            @PathVariable String tenantId,
            @RequestParam(name = "branchId", required = false) String branchId) {
        return ResponseEntity.ok(ApiResponse.success(directoryService.listStaff(tenantId, branchId)));
    }
}
