package com.kvs.auth.controller;

import com.kvs.auth.dto.StaffAccountRequest;
import com.kvs.auth.dto.StaffAccountResult;
import com.kvs.auth.service.StaffAccountService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Sign-in accounts for staff added by the bulk import: IT system administrators and super administrators. */
@RestController
@Slf4j
@RequestMapping("/api/v1/users/staff-accounts")
public class StaffAccountController {

    @Autowired
    private StaffAccountService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IT_SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffAccountResult>>> create(@Valid @RequestBody StaffAccountRequest request) {
        log.info("Creating {} staff sign-in accounts", request.accounts().size());
        List<StaffAccountResult> results = service.create(request);
        long created = results.stream().filter(r -> "CREATED".equals(r.status())).count();
        return ResponseEntity.ok(ApiResponse.success("Created " + created + " of " + results.size() + " accounts", results));
    }
}
