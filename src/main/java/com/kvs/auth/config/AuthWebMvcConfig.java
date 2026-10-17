package com.kvs.auth.config;

import com.kvs.core.config.web.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Custom WebMvc configuration for authservice.
 * Replaces ms-core's default WebConfig to ensure all public auth and password lifecycle endpoints
 * (including reset-password, password-reset, forgot-password, login, register)
 * are properly bypassed by JwtTokenInterceptor before reaching Spring Security and controllers.
 */
@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {

    private final JwtTokenInterceptor jwtTokenInterceptor;

    @Autowired
    public AuthWebMvcConfig(JwtTokenInterceptor jwtTokenInterceptor) {
        this.jwtTokenInterceptor = jwtTokenInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/register",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/password-reset",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/password-change",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/users/login",
                        "/api/v1/users/logout",
                        "/api/v1/users/register",
                        "/api/v1/users/password-reset",
                        "/api/v1/users/reset-password",
                        "/api/v1/users/password-change",
                        "/api/v1/users/forgot-password",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }
}
