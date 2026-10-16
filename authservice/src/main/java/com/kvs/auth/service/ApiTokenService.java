package com.kvs.auth.service;

import com.kvs.auth.dto.ApiTokenResponse;
import com.kvs.auth.dto.GenerateApiTokenRequest;
import com.kvs.auth.entity.ApiToken;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.repo.ApiTokenRepository;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing developer and third-party integration OAuth2 API tokens.
 * Covers user story:
 * US-00007: Generate encrypted oauth2 api tokens for third-party software integrations
 * Actor: Admin Clerk
 */
@Service
@Slf4j
public class ApiTokenService {

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest httpRequest;

    @Transactional
    public ApiTokenResponse generateApiToken(GenerateApiTokenRequest request) {
        log.info("Generating API token for tenantId: {}, user: {}, tokenName: {}",
                request.getTenantId(), request.getUserId(), request.getTokenName());

        String rawToken = "kvs_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now().plus(request.getValidityDays(), ChronoUnit.DAYS);

        ApiToken apiToken = new ApiToken();
        apiToken.setTenantId(request.getTenantId());
        apiToken.setUserId(request.getUserId());
        apiToken.setTokenName(request.getTokenName());
        apiToken.setTokenHash(tokenHash);
        apiToken.setScopes(request.getScopes());
        apiToken.setRateLimitRpm(request.getRateLimitRpm());
        apiToken.setExpiresAt(expiresAt);
        apiToken.setActive(true);

        ApiToken saved = apiTokenRepository.save(apiToken);
        log.info("Generated API token record id: {}", saved.getId());

        logAudit("GENERATE_API_TOKEN", "tenant:" + request.getTenantId(), "SUCCESS",
                "Generated encrypted OAuth2 API token: " + request.getTokenName()
                        + " with scopes: " + request.getScopes()
                        + ", rateLimit: " + request.getRateLimitRpm() + " rpm");

        ApiTokenResponse response = mapToResponse(saved);
        response.setTokenValue(rawToken); // returned only upon initial generation
        return response;
    }

    public List<ApiTokenResponse> getTokens(String tenantId) {
        log.info("Fetching API tokens for tenantId: {}", tenantId);
        return apiTokenRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeToken(Long tokenId) {
        log.info("Revoking API token id: {}", tokenId);
        ApiToken token = apiTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiToken", "id", tokenId));

        token.setActive(false);
        apiTokenRepository.save(token);

        logAudit("REVOKE_API_TOKEN", "token:" + tokenId, "SUCCESS",
                "Revoked API token id " + tokenId);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void logAudit(String action, String resource, String outcome, String details) {
        try {
            String currentUser = SecurityUtils.getCurrentUserEmail().orElse("admin_clerk");
            String ipAddress = (httpRequest != null && httpRequest.getRemoteAddr() != null)
                    ? httpRequest.getRemoteAddr()
                    : "127.0.0.1";

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(currentUser);
            auditLog.setIpAddress(ipAddress);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setOutcome(outcome);
            auditLog.setEventTime(Instant.now());
            auditLog.setDetails(details);

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    private ApiTokenResponse mapToResponse(ApiToken entity) {
        return ApiTokenResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .tokenName(entity.getTokenName())
                .scopes(entity.getScopes())
                .rateLimitRpm(entity.getRateLimitRpm())
                .expiresAt(entity.getExpiresAt())
                .active(entity.isActive())
                .build();
    }
}
