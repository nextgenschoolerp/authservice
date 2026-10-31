package com.kvs.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.auth.dto.IntegrationDtos.*;
import com.kvs.auth.entity.AuditLog;
import com.kvs.auth.entity.TenantIntegration;
import com.kvs.auth.repo.AuditLogRepository;
import com.kvs.auth.repo.TenantIntegrationRepository;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ConflictException;
import com.kvs.core.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Settings → Integrations: a school's Email (SMTP), SMS (MSG91 or Twilio) and WhatsApp (Meta Cloud API) providers, and
 * the payment gateway (not available yet). Saves the settings with the secret encrypted, and sends a test message through
 * the saved provider. Live messages of the other services still use the server configuration (kvs.notification.*).
 */
@Service
@Slf4j
public class IntegrationService {

    /** A provider: its required and optional fields (name → label), and what its secret is called. */
    record Provider(String code, String label, Map<String, String> required, Map<String, String> optional, String secretLabel) { }

/** group: the tab it belongs to; EMAIL_ADMISSIONS and EMAIL_FINANCE are extra mailboxes of the EMAIL group */
    record Channel(String code, String label, boolean available, List<Provider> providers, String group) {
        Channel(String code, String label, boolean available, List<Provider> providers) { this(code, label, available, providers, code); }
    }

    // username and password are required; the From address is optional and defaults to the username
    private static final Provider SMTP = new Provider("SMTP", "SMTP server", ordered("host", "SMTP host", "port", "Port", "username", "SMTP USERNAME", "security", "Security"),
            ordered("fromAddress", "From address", "fromName", "From name"), "SMTP PASSWORD");

    /** Mailboxes a service may ask for (purpose), sent from instead of the school's main account when set up and switched on */
    public static final Map<String, String> PURPOSES = Map.of("ADMISSIONS", "EMAIL_ADMISSIONS", "FINANCE", "EMAIL_FINANCE");

    /** Who may send through each purpose's mailbox: the roles that send those messages in admissionservice and financeservice */
    private static final Map<String, Set<String>> PURPOSE_ROLES = Map.of(
            "ADMISSIONS", Set.of("ADMISSION_OFFICER", "FRONT_OFFICE_COUNSELOR", "SUPER_ADMIN"),
            "FINANCE", Set.of("FINANCE_CASHIER", "CHIEF_ACCOUNTANT", "ACCOUNTS_CLERK", "APPLICANT_PARENT", "SUPER_ADMIN"),
            "CIRCULARS", Set.of("PUBLIC_RELATIONS_OFFICER", "COMMUNICATION_OFFICER", "PRINCIPAL", "SCHOOL_PRINCIPAL", "SUPER_ADMIN", "ADMIN"));

    private static final List<Channel> CHANNELS = List.of(
            new Channel("EMAIL", "School email", true, List.of(SMTP), "EMAIL"),
            new Channel("EMAIL_ADMISSIONS", "Admissions email", true, List.of(SMTP), "EMAIL"),
            new Channel("EMAIL_FINANCE", "Finance email", true, List.of(SMTP), "EMAIL"),
            new Channel("SMS", "SMS", true, List.of(
                    new Provider("MSG91", "MSG91", ordered("senderId", "Sender ID (6 letters)", "templateId", "DLT template ID"), Map.of(), "Auth key"),
                    new Provider("TWILIO", "Twilio", ordered("accountSid", "Account SID", "fromNumber", "From number (+E.164)"), Map.of(), "Auth token"))),
            new Channel("WHATSAPP", "WhatsApp", true, List.of(
                    new Provider("META_CLOUD", "WhatsApp Cloud API (Meta)", ordered("phoneNumberId", "Phone number ID"),
                            ordered("businessAccountId", "WhatsApp Business account ID", "testTemplate", "Test template", "broadcastTemplate", "Circular template ({{1}} title, {{2}} message)", "templateLanguage", "Template language"), "Access token"))),
            new Channel("PAYMENT", "Payment gateway", false, List.of(
                    new Provider("RAZORPAY", "Razorpay", Map.of(), Map.of(), "Key secret"),
                    new Provider("PAYU", "PayU", Map.of(), Map.of(), "Salt"),
                    new Provider("CCAVENUE", "CCAvenue", Map.of(), Map.of(), "Working key"))));

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");

    @Autowired private TenantIntegrationRepository repository;
    @Autowired private IntegrationSecretCipher cipher;
    @Autowired private TenantAccessGuard tenantGuard;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ObjectMapper objectMapper;

    private static Map<String, String> ordered(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static Channel channel(String code) {
        return CHANNELS.stream().filter(c -> c.code().equalsIgnoreCase(code)).findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown integration " + code + ": use EMAIL, EMAIL_ADMISSIONS, EMAIL_FINANCE, SMS, WHATSAPP or PAYMENT"));
    }

    @Transactional(readOnly = true)
    public List<IntegrationResponse> list(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        Map<String, TenantIntegration> saved = new HashMap<>();
        repository.findByTenantId(tenantId).forEach(i -> saved.put(i.getChannel(), i));
        return CHANNELS.stream().map(c -> toResponse(c, saved.get(c.code()))).toList();
    }

    @Transactional
    public IntegrationResponse save(String tenantId, String channelCode, SaveIntegrationRequest request) {
        tenantGuard.requireTenant(tenantId);
        Channel c = channel(channelCode);
        if (!c.available()) throw new ConflictException(c.label() + " integration is not available yet. It will be added in a later release.");
        Provider p = c.providers().stream().filter(x -> x.code().equalsIgnoreCase(request.provider())).findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown " + c.label() + " provider " + request.provider()));

        TenantIntegration i = repository.findByTenantIdAndChannel(tenantId, c.code()).orElseGet(() -> {
            TenantIntegration n = new TenantIntegration();
            n.setTenantId(tenantId);
            n.setChannel(c.code());
            return n;
        });
        boolean providerChanged = i.getProvider() != null && !i.getProvider().equals(p.code());

        // keep only this provider's fields, trimmed; check the required ones and their format
        Map<String, String> in = request.settings() == null ? Map.of() : request.settings();
        Map<String, String> settings = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (String k : p.required().keySet()) {
            String v = trim(in.get(k));
            if (v == null) problems.add(p.required().get(k) + " is required");
            else settings.put(k, v);
        }
        for (String k : p.optional().keySet()) { String v = trim(in.get(k)); if (v != null) settings.put(k, v); }
        validate(p.code(), settings, problems);

        String secret = trim(request.secret());
        boolean hasSecret = secret != null || (!providerChanged && i.getSecretCipher() != null);
        if (!hasSecret) problems.add(p.secretLabel() + " is required");
        if (!problems.isEmpty()) throw new BadRequestException(String.join("; ", problems));

        i.setProvider(p.code());
        i.setEnabled(request.enabled());
        i.setSettingsJson(write(settings));
        if (secret != null) {
            i.setSecretCipher(cipher.encrypt(secret));
            i.setSecretHint(secret.length() <= 4 ? "" : secret.substring(secret.length() - 4));
        }
        // a change means the last test no longer proves anything
        i.setStatus("SAVED");
        i.setLastTestResult(null);
        i.setLastTestAt(null);
        TenantIntegration saved = repository.save(i);
        audit("SAVE_INTEGRATION", tenantId, c.code() + " via " + p.code() + (request.enabled() ? " (enabled)" : " (disabled)")
                + (secret != null ? "; secret replaced" : ""));
        return toResponse(c, saved);
    }

    @Transactional
    public void remove(String tenantId, String channelCode) {
        tenantGuard.requireTenant(tenantId);
        Channel c = channel(channelCode);
        repository.findByTenantIdAndChannel(tenantId, c.code()).ifPresent(i -> {
            repository.delete(i);
            audit("REMOVE_INTEGRATION", tenantId, c.code() + " settings and secret removed");
        });
    }

    /** Sends one test message through the saved provider and records the outcome on the integration. */
    @Transactional
    public TestIntegrationResponse test(String tenantId, String channelCode, TestIntegrationRequest request) {
        tenantGuard.requireTenant(tenantId);
        Channel c = channel(channelCode);
        if (!c.available()) throw new ConflictException(c.label() + " integration is not available yet.");
        TenantIntegration i = repository.findByTenantIdAndChannel(tenantId, c.code())
                .orElseThrow(() -> new BadRequestException("Save the " + c.label() + " settings before sending a test"));
        Map<String, String> s = read(i.getSettingsJson());
        String recipient = request.recipient().trim();
        String secret = cipher.decrypt(i.getSecretCipher());

        boolean ok;
        String message;
        try {
            message = switch (i.getProvider()) {
                case "SMTP" -> sendEmail(s, secret, recipient);
                case "MSG91" -> sendMsg91(s, secret, mobile(recipient));
                case "TWILIO" -> sendTwilio(s, secret, mobile(recipient));
                case "META_CLOUD" -> sendWhatsApp(s, secret, mobile(recipient));
                default -> throw new BadRequestException("No test is available for " + i.getProvider());
            };
            ok = true;
        } catch (BadRequestException e) {
            throw e;
        } catch (RestClientResponseException e) {
            ok = false;
            message = "The provider refused the message (HTTP " + e.getStatusCode().value() + "): " + clip(e.getResponseBodyAsString());
        } catch (Exception e) {
            ok = false;
            message = "Could not send: " + reason(e, s);
        }
        message = message.replace(secret, "••••");
        i.setStatus(ok ? "VERIFIED" : "FAILED");
        i.setLastTestAt(Instant.now());
        i.setLastTestResult(clip(message));
        TenantIntegration saved = repository.save(i);
        audit("TEST_INTEGRATION", tenantId, c.code() + " via " + i.getProvider() + " to " + recipient + ": " + (ok ? "sent" : "failed"));
        return new TestIntegrationResponse(ok, message, toResponse(c, saved));
    }

    /* ---------------- providers ---------------- */

    private static final String TEST_TEXT = "This is a test message from School ERP (Settings → Integrations). No action is needed.";

    private String sendEmail(Map<String, String> s, String password, String to) {
        if (!EMAIL.matcher(to).matches()) throw new BadRequestException("Enter a valid email address to send the test to");
        email(mailer(s, password), s, to, "Test message from School ERP", TEST_TEXT);
        return "Test email sent to " + to + " through " + s.get("host") + ".";
    }

    private String sendMsg91(Map<String, String> s, String authKey, String mobile) {
        String id = msg91(s, authKey, mobile, "Test message", TEST_TEXT);
        return "Test SMS accepted by MSG91 for " + mobile + "." + (id.isEmpty() ? "" : " Request " + id + ".");
    }

    private String sendTwilio(Map<String, String> s, String token, String mobile) {
        twilio(s, token, mobile, TEST_TEXT);
        return "Test SMS accepted by Twilio for " + mobile + ".";
    }

    private String sendWhatsApp(Map<String, String> s, String token, String mobile) {
        whatsapp(s, token, mobile, s.getOrDefault("testTemplate", "hello_world"), List.of(), null);
        return "Test WhatsApp template \"" + s.getOrDefault("testTemplate", "hello_world") + "\" accepted for " + mobile + ".";
    }

    private static JavaMailSenderImpl mailer(Map<String, String> s, String password) {
        JavaMailSenderImpl mail = new JavaMailSenderImpl();
        mail.setHost(s.get("host"));
        mail.setPort(Integer.parseInt(s.get("port")));
        if (s.get("username") != null) { mail.setUsername(s.get("username")); mail.setPassword(password); }
        Properties p = mail.getJavaMailProperties();
        String security = s.getOrDefault("security", "STARTTLS").toUpperCase(Locale.ROOT);
        p.put("mail.smtp.auth", String.valueOf(s.get("username") != null));
        p.put("mail.smtp.starttls.enable", String.valueOf(security.equals("STARTTLS")));
        p.put("mail.smtp.ssl.enable", String.valueOf(security.equals("SSL")));
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.writetimeout", "10000");
        return mail;
    }

    private static void email(JavaMailSenderImpl mail, Map<String, String> s, String to, String subject, String text) {
        email(mail, s, to, subject, text, List.of());
    }

    /** Plain text, with attachments (e.g. the fee receipt PDF) when given */
    private static void email(JavaMailSenderImpl mail, Map<String, String> s, String to, String subject, String text, List<DeliveryAttachment> files) {
        String from = s.get("fromAddress") != null ? s.get("fromAddress") : s.get("username");
        String sender = s.get("fromName") != null ? s.get("fromName") + " <" + from + ">" : from;
        if (files == null || files.isEmpty()) {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(sender);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            mail.send(m);
            return;
        }
        try {
            jakarta.mail.internet.MimeMessage m = mail.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper h = new org.springframework.mail.javamail.MimeMessageHelper(m, true, "UTF-8");
            h.setFrom(new jakarta.mail.internet.InternetAddress(from, s.get("fromName"), "UTF-8"));
            h.setTo(to);
            h.setSubject(subject);
            h.setText(text);
            for (DeliveryAttachment f : files) {
                h.addAttachment(f.filename(), new org.springframework.core.io.ByteArrayResource(Base64.getDecoder().decode(f.contentBase64())),
                        f.contentType() == null ? "application/octet-stream" : f.contentType());
            }
            mail.send(m);
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not build the email: " + e.getMessage(), e);
        }
    }

    /** Internal service calls (scheduled admission reminders) may send for admissions and finance only; users need that area's role. */
    private void authorise(String purpose) {
        Set<String> allowed = PURPOSE_ROLES.get(purpose);
        if (allowed == null) throw new BadRequestException("Unknown purpose " + purpose + ": use CIRCULARS, ADMISSIONS or FINANCE");
        List<String> roles = SecurityUtils.getCurrentUserRoles();
        if (roles.contains("ROLE_INTERNAL_SERVICE") && !purpose.equals("CIRCULARS")) return;
        if (roles.stream().map(r -> r.replaceFirst("^ROLE_", "")).noneMatch(allowed::contains)) {
            throw new com.kvs.core.exception.ForbiddenException(purpose.equals("CIRCULARS") ? "You may not send circulars" : "You may not send " + purpose.toLowerCase(Locale.ROOT) + " messages");
        }
    }

    /** MSG91 Flow: the DLT template may use ##title## and ##message## (also sent as ##var1## and ##var2##). Returns the request id. */
    private String msg91(Map<String, String> s, String authKey, String mobile, String title, String message) {
        Map<String, Object> recipient = Map.of("mobiles", mobile.substring(1), "title", title, "message", message, "var1", title, "var2", message);
        Map<String, Object> body = Map.of("template_id", s.get("templateId"), "short_url", "0", "recipients", List.of(recipient));
        String reply = client().post().uri("https://control.msg91.com/api/v5/flow/")
                .header("authkey", authKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
        return field(reply, "message");
    }

    /** Returns the Twilio message SID. */
    private String twilio(Map<String, String> s, String token, String mobile, String text) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("To", mobile);
        form.add("From", s.get("fromNumber"));
        form.add("Body", text);
        String reply = client().post().uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", s.get("accountSid"))
                .headers(h -> h.setBasicAuth(s.get("accountSid"), token)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(String.class);
        return field(reply, "sid");
    }

    /**
     * An approved template (params fill {{1}}, {{2}} …) or, when template is null, free text (Meta accepts free text only within
     * 24 hours of the parent's last message). Returns the WhatsApp message id.
     */
    private String whatsapp(Map<String, String> s, String token, String mobile, String template, List<String> params, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", mobile.substring(1));
        if (template != null) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", template);
            t.put("language", Map.of("code", s.getOrDefault("templateLanguage", "en_US")));
            if (!params.isEmpty()) t.put("components", List.of(Map.of("type", "body",
                    "parameters", params.stream().map(p -> Map.of("type", "text", "text", p)).toList())));
            body.put("type", "template");
            body.put("template", t);
        } else {
            body.put("type", "text");
            body.put("text", Map.of("preview_url", false, "body", text));
        }
        String reply = client().post().uri("https://graph.facebook.com/v20.0/{id}/messages", s.get("phoneNumberId"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
        try { return objectMapper.readTree(reply == null ? "{}" : reply).path("messages").path(0).path("id").asText(""); } catch (Exception e) { return ""; }
    }

    private String field(String json, String name) {
        try { return objectMapper.readTree(json == null ? "{}" : json).path(name).asText(""); } catch (Exception e) { return ""; }
    }

    /* ---------------- bulk delivery (Communications) ---------------- */

    /** Which channels this school can send on now (saved, with a secret, and switched on). Shown to whoever writes circulars. */
    @Transactional(readOnly = true)
    public List<ChannelReadiness> readiness(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        Map<String, TenantIntegration> saved = new HashMap<>();
        repository.findByTenantId(tenantId).forEach(i -> saved.put(i.getChannel(), i));
        return CHANNELS.stream().filter(c -> c.available() && c.code().equals(c.group())).map(c -> {
            TenantIntegration i = saved.get(c.code());
            boolean ready = i != null && i.isEnabled() && i.getSecretCipher() != null;
            return new ChannelReadiness(c.code(), c.label(), ready, i == null ? null : i.getProvider(), i == null ? "NOT_CONFIGURED" : i.getStatus());
        }).toList();
    }

    /**
     * Sends each message through the school's own provider for the channel, one by one, and reports each outcome:
     * SENT (accepted by the provider), FAILED (refused or unreachable) or SKIPPED (no usable provider, or no valid address).
     * Nothing is retried here; Communications decides about the SMS fallback.
     */
    public DeliverResponse deliver(String tenantId, DeliverRequest request) {
        tenantGuard.requireTenant(tenantId);
        String purpose = request.purpose() == null || request.purpose().isBlank() ? "CIRCULARS" : request.purpose().trim().toUpperCase(Locale.ROOT);
        authorise(purpose);
        Channel requested = channel(request.channel());
        if (!requested.available()) throw new ConflictException(requested.label() + " integration is not available yet.");
        // email: the purpose's own mailbox when it is set up and on, else the school's main email account
        List<Channel> candidates = new ArrayList<>();
        if (requested.code().equals("EMAIL") && PURPOSES.containsKey(purpose)) candidates.add(channel(PURPOSES.get(purpose)));
        candidates.add(requested);
        Channel c = null;
        TenantIntegration i = null;
        for (Channel k : candidates) {
            TenantIntegration t = repository.findByTenantIdAndChannel(tenantId, k.code()).orElse(null);
            if (t != null && t.isEnabled() && t.getSecretCipher() != null) { c = k; i = t; break; }
        }
        List<DeliveryResult> out = new ArrayList<>();
        if (i == null) {
            String why = candidates.size() > 1
                    ? "No email account is set up and switched on for " + purpose.toLowerCase(Locale.ROOT) + " or for the school (Settings → Integrations → Email)"
                    : requested.label() + " is not set up or is switched off for this school (Settings → Integrations)";
            request.messages().forEach(m -> out.add(new DeliveryResult(m.ref(), "SKIPPED", null, why)));
            return new DeliverResponse(requested.code(), null, out);
        }
        Map<String, String> s = read(i.getSettingsJson());
        String secret = cipher.decrypt(i.getSecretCipher());
        JavaMailSenderImpl mail = i.getProvider().equals("SMTP") ? mailer(s, secret) : null;
        int sent = 0, failed = 0, skipped = 0;
        for (DeliveryMessage m : request.messages()) {
            String to = m.recipient() == null ? "" : m.recipient().trim();
            String title = m.title() == null ? "" : m.title();
            DeliveryResult r;
            try {
                String id = switch (i.getProvider()) {
                    case "SMTP" -> {
                        if (!EMAIL.matcher(to).matches()) throw new IllegalArgumentException("No valid email address");
                        email(mail, s, to, m.subject() != null ? m.subject() : title, m.body(), m.attachments());
                        yield "";
                    }
                    case "MSG91" -> msg91(s, secret, mobile(to), title, m.body());
                    case "TWILIO" -> twilio(s, secret, mobile(to), (title.isEmpty() ? "" : title + "\n") + m.body());
                    case "META_CLOUD" -> s.get("broadcastTemplate") != null
                            ? whatsapp(s, secret, mobile(to), s.get("broadcastTemplate"), List.of(title, m.body()), null)
                            : whatsapp(s, secret, mobile(to), null, List.of(), (title.isEmpty() ? "" : "*" + title + "*\n") + m.body());
                    default -> throw new IllegalStateException("Unknown provider " + i.getProvider());
                };
                r = new DeliveryResult(m.ref(), "SENT", id == null || id.isEmpty() ? null : id, null);
                sent++;
            } catch (BadRequestException e) {
                r = new DeliveryResult(m.ref(), "SKIPPED", null, "No valid mobile number");
                skipped++;
            } catch (IllegalArgumentException e) {
                r = new DeliveryResult(m.ref(), "SKIPPED", null, e.getMessage());
                skipped++;
            } catch (RestClientResponseException e) {
                r = new DeliveryResult(m.ref(), "FAILED", null, clip(("HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString()).replace(secret, "••••")));
                failed++;
            } catch (Exception e) {
                r = new DeliveryResult(m.ref(), "FAILED", null, clip(reason(e, s).replace(secret, "••••")));
                failed++;
            }
            out.add(r);
        }
        audit("DELIVER_MESSAGES", tenantId, c.code() + " via " + i.getProvider() + ": " + sent + " sent, " + failed + " failed, " + skipped + " skipped"
                + (request.reference() != null ? " (" + clip(request.reference()) + ")" : ""));
        return new DeliverResponse(c.code(), i.getProvider(), out);
    }

    private static RestClient client() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(15_000);
        return RestClient.builder().requestFactory(f).build();
    }

    /** A short reason for a failed send, from the innermost cause (connection refused, unknown host, login refused, timeout). */
    private static String reason(Exception e, Map<String, String> s) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String where = s.get("host") != null ? s.get("host") + ":" + s.get("port") : "the provider";
        if (e instanceof org.springframework.mail.MailAuthenticationException) return "the server at " + where + " refused the username or password.";
        if (root instanceof java.net.ConnectException) return "could not connect to " + where + " (connection refused). Check the host, port and security.";
        if (root instanceof java.net.UnknownHostException) return "the host " + root.getMessage() + " was not found. Check the spelling.";
        if (root instanceof java.net.SocketTimeoutException) return where + " did not answer in time. Check the port and security, or a firewall.";
        if (root instanceof javax.net.ssl.SSLException) return "the secure connection to " + where + " failed (" + clip(root.getMessage()) + "). Try the other security option.";
        return clip(String.valueOf(root.getMessage()));
    }

    /* ---------------- checks and mapping ---------------- */

    private static void validate(String provider, Map<String, String> s, List<String> problems) {
        switch (provider) {
            case "SMTP" -> {
                if (s.get("port") != null && !s.get("port").matches("\\d{1,5}") || (s.get("port") != null && Integer.parseInt(s.get("port")) > 65535)) problems.add("Port must be a number from 1 to 65535");
                if (s.get("security") != null && !Set.of("STARTTLS", "SSL", "NONE").contains(s.get("security").toUpperCase(Locale.ROOT))) problems.add("Security must be STARTTLS, SSL or NONE");
                else if (s.get("security") != null) s.put("security", s.get("security").toUpperCase(Locale.ROOT));
                if (s.get("fromAddress") != null && !EMAIL.matcher(s.get("fromAddress")).matches()) problems.add("From address is not a valid email address");
                // port 465 is implicit TLS (SSL); port 587 upgrades a plain connection (STARTTLS). Mixing them fails at send time.
                if ("465".equals(s.get("port")) && "STARTTLS".equals(s.get("security"))) problems.add("Port 465 uses SSL; for STARTTLS use port 587");
                if ("587".equals(s.get("port")) && "SSL".equals(s.get("security"))) problems.add("Port 587 uses STARTTLS; for SSL use port 465");
            }
            case "MSG91" -> { if (s.get("senderId") != null && !s.get("senderId").matches("[A-Za-z]{6}")) problems.add("Sender ID must be 6 letters"); }
            case "TWILIO" -> {
                if (s.get("accountSid") != null && !s.get("accountSid").matches("AC[0-9a-fA-F]{32}")) problems.add("Account SID must look like AC followed by 32 hexadecimal characters");
                if (s.get("fromNumber") != null && !s.get("fromNumber").matches("\\+\\d{8,15}")) problems.add("From number must be in +E.164 form, e.g. +14155550100");
            }
            case "META_CLOUD" -> { if (s.get("phoneNumberId") != null && !s.get("phoneNumberId").matches("\\d{6,20}")) problems.add("Phone number ID must be digits"); }
            default -> { }
        }
    }

    /** +91XXXXXXXXXX from a 10-digit Indian mobile (with or without +91 / spaces), or an E.164 number */
    private static String mobile(String v) {
        String d = v.replaceAll("[\\s-]", "");
        if (d.matches("[6-9]\\d{9}")) return "+91" + d;
        if (d.matches("(\\+?91)[6-9]\\d{9}")) return "+" + d.replaceFirst("^\\+", "");
        if (d.matches("\\+\\d{8,15}")) return d;
        throw new BadRequestException("Enter a 10-digit mobile number (or +country number) to send the test to");
    }

    private IntegrationResponse toResponse(Channel c, TenantIntegration i) {
        List<String> providers = c.providers().stream().map(Provider::code).toList();
        if (i == null) {
            return new IntegrationResponse(c.code(), c.label(), c.available(), null, providers, false, c.available() ? "NOT_CONFIGURED" : "PENDING",
                    Map.of(), false, null, null, null, null, null, c.group());
        }
        return new IntegrationResponse(c.code(), c.label(), c.available(), i.getProvider(), providers, i.isEnabled(),
                c.available() ? i.getStatus() : "PENDING", read(i.getSettingsJson()), i.getSecretCipher() != null, i.getSecretHint(),
                i.getLastTestAt(), i.getLastTestResult(), i.getUpdatedBy() != null ? i.getUpdatedBy() : i.getCreatedBy(),
                i.getUpdatedAt() != null ? i.getUpdatedAt() : i.getCreatedAt(), c.group());
    }

    /** Field definitions for the screen: channel → provider → [required fields, optional fields, secret label]. */
    public Map<String, Object> catalog() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Channel c : CHANNELS) {
            Map<String, Object> ps = new LinkedHashMap<>();
            for (Provider p : c.providers()) ps.put(p.code(), Map.of("label", p.label(), "required", p.required(), "optional", p.optional(), "secretLabel", p.secretLabel()));
            out.put(c.code(), Map.of("label", c.label(), "available", c.available(), "providers", ps));
        }
        return out;
    }

    private static String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static String clip(String s) { return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s; }

    private String write(Map<String, String> m) {
        try { return objectMapper.writeValueAsString(m); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private Map<String, String> read(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() { }); }
        catch (Exception e) { return new LinkedHashMap<>(); }
    }

    private void audit(String action, String tenantId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setUserId(SecurityUtils.getCurrentUserEmail().orElse("admin"));
            a.setIpAddress("127.0.0.1");
            a.setAction(action);
            a.setResource("tenant:" + tenantId + ":integrations");
            a.setOutcome("SUCCESS");
            a.setEventTime(Instant.now());
            a.setDetails(details);
            auditLogRepository.save(a);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
