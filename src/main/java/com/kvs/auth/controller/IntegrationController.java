package com.kvs.auth.controller;

import com.kvs.auth.dto.IntegrationDtos.*;
import com.kvs.auth.service.IntegrationService;
import com.kvs.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Settings → Integrations: Email (SMTP), SMS (MSG91, Twilio), WhatsApp (Meta Cloud API); payment gateway pending.
 * Principals may look; super administrators and IT system administrators change settings and send tests.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tenants/{tenantId}/integrations")
public class IntegrationController {

    private static final String VIEW = "hasAnyRole('SUPER_ADMIN', 'IT_SYSTEM_ADMIN', 'PRINCIPAL', 'SCHOOL_PRINCIPAL') or hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_IT_SYSTEM_ADMIN', 'ROLE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL')";
    private static final String EDIT = "hasAnyRole('SUPER_ADMIN', 'IT_SYSTEM_ADMIN') or hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_IT_SYSTEM_ADMIN')";

    @Autowired
    private IntegrationService service;

    @GetMapping
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<List<IntegrationResponse>>> list(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(service.list(tenantId)));
    }

    /** The fields each provider needs, for the screen */
    @GetMapping("/catalog")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalog(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(service.catalog()));
    }

    /** Who writes circulars (communicationservice BROADCAST_MANAGE) may see which channels are ready and send through them */
    private static final String SEND = "hasAnyRole('PUBLIC_RELATIONS_OFFICER', 'COMMUNICATION_OFFICER', 'PRINCIPAL', 'SCHOOL_PRINCIPAL', 'SUPER_ADMIN', 'ADMIN') or hasAnyAuthority('ROLE_PUBLIC_RELATIONS_OFFICER', 'ROLE_COMMUNICATION_OFFICER', 'ROLE_PRINCIPAL', 'ROLE_SCHOOL_PRINCIPAL', 'ROLE_SUPER_ADMIN', 'ROLE_ADMIN')";

    @GetMapping("/readiness")
    @PreAuthorize(SEND)
    public ResponseEntity<ApiResponse<List<ChannelReadiness>>> readiness(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(service.readiness(tenantId)));
    }

    /**
     * Called by communicationservice (circulars), financeservice (fee receipts) and admissionservice (offers, reminders) with the
     * user's own token, or by a scheduled job with the internal service key (X-Internal-Key)
     */
    @PostMapping("/deliver")
    @PreAuthorize("isAuthenticated()") // the purpose decides who may send (IntegrationService.authorise)
    public ResponseEntity<ApiResponse<DeliverResponse>> deliver(@PathVariable("tenantId") String tenantId, @Valid @RequestBody DeliverRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.deliver(tenantId, request)));
    }

    @PutMapping("/{channel}")
    @PreAuthorize(EDIT)
    public ResponseEntity<ApiResponse<IntegrationResponse>> save(@PathVariable("tenantId") String tenantId, @PathVariable("channel") String channel,
                                                                 @Valid @RequestBody SaveIntegrationRequest request) {
        log.info("Saving {} integration ({}) for tenant {}", channel, request.provider(), tenantId);
        return ResponseEntity.ok(ApiResponse.success("Integration saved", service.save(tenantId, channel.toUpperCase(), request)));
    }

    @PostMapping("/{channel}/test")
    @PreAuthorize(EDIT)
    public ResponseEntity<ApiResponse<TestIntegrationResponse>> test(@PathVariable("tenantId") String tenantId, @PathVariable("channel") String channel,
                                                                     @Valid @RequestBody TestIntegrationRequest request) {
        TestIntegrationResponse r = service.test(tenantId, channel.toUpperCase(), request);
        return ResponseEntity.ok(ApiResponse.success(r.ok() ? "Test message sent" : "Test message failed", r));
    }

    @DeleteMapping("/{channel}")
    @PreAuthorize(EDIT)
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable("tenantId") String tenantId, @PathVariable("channel") String channel) {
        service.remove(tenantId, channel.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Integration removed", null));
    }
}
