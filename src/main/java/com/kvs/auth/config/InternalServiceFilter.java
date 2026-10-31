package com.kvs.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Lets another service send email without a signed-in user (admissionservice's scheduled reminders): a request to
 * POST /api/v1/tenants/{id}/integrations/deliver carrying the shared key in X-Internal-Key is authenticated as
 * ROLE_INTERNAL_SERVICE. Accepted on that one endpoint only. Key: kvs.internal.key (env KVS_INTERNAL_KEY), the same value
 * in every service; the development default must be replaced outside local development.
 */
@Slf4j
public class InternalServiceFilter extends OncePerRequestFilter {

    private final byte[] key;

    public InternalServiceFilter(String key) {
        this.key = key == null || key.isBlank() ? null : key.getBytes(StandardCharsets.UTF_8);
        if ("schoolerp-dev-internal-key".equals(key)) log.warn("kvs.internal.key (KVS_INTERNAL_KEY) uses the development default. Set it outside local development.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String given = request.getHeader("X-Internal-Key");
        if (given != null && key != null && SecurityContextHolder.getContext().getAuthentication() == null
                && "POST".equals(request.getMethod()) && request.getRequestURI().matches(".*/api/v1/tenants/[^/]+/integrations/deliver")
                && MessageDigest.isEqual(key, given.getBytes(StandardCharsets.UTF_8))) {
            String caller = request.getHeader("X-Internal-Service");
            var auth = new UsernamePasswordAuthenticationToken("service:" + (caller == null ? "unknown" : caller), null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
