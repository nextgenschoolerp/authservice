package com.kvs.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Settings → Integrations: request and response shapes. Secrets are write-only; responses carry only a hint. */
public final class IntegrationDtos {

    private IntegrationDtos() { }

    /**
     * One channel of a school. available=false for channels that cannot be configured yet (PAYMENT).
     * status: NOT_CONFIGURED, SAVED (not tested since the last change), VERIFIED, FAILED or PENDING (not available yet).
     */
    public record IntegrationResponse(String channel, String label, boolean available, String provider, List<String> providers,
                                      boolean enabled, String status, Map<String, String> settings, boolean secretSaved,
                                      String secretHint, Instant lastTestAt, String lastTestResult, String updatedBy, Instant updatedAt,
                                      String group) { }

    /** secret: leave blank to keep the saved one */
    public record SaveIntegrationRequest(@NotBlank(message = "Choose a provider") String provider, boolean enabled,
                                         Map<String, String> settings, @Size(max = 500) String secret) { }

    /** recipient: an email address (EMAIL) or a 10-digit Indian mobile number (SMS, WHATSAPP) */
    public record TestIntegrationRequest(@NotBlank(message = "Enter where to send the test message") String recipient) { }

    public record TestIntegrationResponse(boolean ok, String message, IntegrationResponse integration) { }

    /** Whether a channel can send now: saved, with a secret, and switched on */
    public record ChannelReadiness(String channel, String label, boolean ready, String provider, String status) { }

    /** ref: the caller's id for the message (Communications: the delivery log id); title and subject are optional */
    public record DeliveryMessage(String ref, String recipient, String subject, String title, @NotBlank String body, List<DeliveryAttachment> attachments) { }

    /** A file sent with an email, base64 encoded */
    public record DeliveryAttachment(@NotBlank String filename, String contentType, @NotBlank String contentBase64) { }

    /**
     * Up to 200 messages on one channel; reference describes the batch for the audit log (e.g. "circular 1234").
     * purpose: CIRCULARS (default), ADMISSIONS or FINANCE. Email for ADMISSIONS / FINANCE goes from that mailbox when it is set up,
     * else from the school's main email account.
     */
    public record DeliverRequest(@NotBlank String channel, String reference, String purpose, @NotEmpty @Size(max = 200) List<@Valid DeliveryMessage> messages) { }

    /** status: SENT, FAILED or SKIPPED; providerId is the provider's message id when it gives one */
    public record DeliveryResult(String ref, String status, String providerId, String error) { }

    /** channel: the account used, e.g. EMAIL_FINANCE, or EMAIL when the school's main account was used */
    public record DeliverResponse(String channel, String provider, List<DeliveryResult> results) { }
}
