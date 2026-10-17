package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Successful authentication response payload containing JWT token and session identity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String userId;
    private String username;
    private String email;
    private String tenantId;
    private String userType;
    private List<String> roles;
    private boolean mfaRequired;
    private String sessionId;
    private boolean mustChangePassword;
    private String message;
}
