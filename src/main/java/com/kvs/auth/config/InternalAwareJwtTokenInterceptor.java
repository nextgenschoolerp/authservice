package com.kvs.auth.config;

import com.kvs.core.config.web.JwtTokenInterceptor;
import com.kvs.core.service.JwtTokenService;
import com.kvs.core.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * ms-core's JWT interceptor, except for a call that {@link InternalServiceFilter} authenticated with the internal key
 * (a scheduled job sending email): it carries no JWT. Primary, so every configuration that registers the JWT interceptor
 * (AuthWebMvcConfig, and ms-core's WebConfig if it is picked up) uses this one.
 */
@Component
@Primary
public class InternalAwareJwtTokenInterceptor extends JwtTokenInterceptor {

    public InternalAwareJwtTokenInterceptor(JwtTokenService jwtTokenService) {
        super(jwtTokenService);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (SecurityUtils.getCurrentUserRoles().contains("ROLE_INTERNAL_SERVICE")) return true;
        return super.preHandle(request, response, handler);
    }
}
