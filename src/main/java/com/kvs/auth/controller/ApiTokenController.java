package com.kvs.auth.controller;

import com.kvs.auth.dto.ApiTokenResponse;
import com.kvs.auth.dto.GenerateApiTokenRequest;
import com.kvs.auth.service.ApiTokenService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller managing developer and third-party integration API tokens.
 * Covers user story:
 * US-00007: Generate encrypted oauth2 api tokens for third-party software integrations
 * Actor: Admin Clerk
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/integrations/api-tokens")
public class ApiTokenController {

    @Autowired
    private ApiTokenService apiTokenService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_CLERK') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_ADMIN_CLERK') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ApiTokenResponse>> generateApiToken(
            @Valid @RequestBody GenerateApiTokenRequest request) {
        log.info("Request to generate API token for user: {}", request.getUserId());
        ApiTokenResponse response = apiTokenService.generateApiToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("API token generated successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_CLERK') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_ADMIN_CLERK') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ApiTokenResponse>>> getTokens(
            @RequestParam("tenantId") String tenantId) {
        log.info("Request to fetch API tokens for tenant: {}", tenantId);
        List<ApiTokenResponse> response = apiTokenService.getTokens(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{tokenId}")
    @PreAuthorize("hasRole('ADMIN_CLERK') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_ADMIN_CLERK') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @PathVariable("tokenId") Long tokenId) {
        log.info("Request to revoke API token id: {}", tokenId);
        apiTokenService.revokeToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success("API token revoked successfully", null));
    }
}
