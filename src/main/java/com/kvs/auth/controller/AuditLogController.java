package com.kvs.auth.controller;

import com.kvs.auth.dto.AuditLogResponse;
import com.kvs.auth.service.AuditLogService;
import com.kvs.core.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Controller providing real-time query access to system audit logs.
 * Covers user story:
 * US-00005: Query real-time system audit logs filtered by ip address and user id
 * Actor: Security Auditor
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SECURITY_AUDITOR') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SECURITY_AUDITOR') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> queryAuditLogs(
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "ipAddress", required = false) String ipAddress,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        log.info("Request to query audit logs - userId: {}, ipAddress: {}", userId, ipAddress);
        List<AuditLogResponse> logs = auditLogService.queryAuditLogs(userId, ipAddress, from, to);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/high-privilege-logs")
    @PreAuthorize("hasRole('ADMIN_CLERK') or hasRole('SECURITY_AUDITOR') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_ADMIN_CLERK') or hasAuthority('ROLE_SECURITY_AUDITOR') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> queryHighPrivilegeLogs(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        log.info("Request to query high-privilege audit logs");
        List<AuditLogResponse> logs = auditLogService.queryHighPrivilegeLogs(from, to);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
