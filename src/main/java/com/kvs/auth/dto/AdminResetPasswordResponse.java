package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordResponse {
    private String userId;
    private String username;
    private String temporaryPassword;
    private boolean mustChangePassword;
    private String message;
    private Instant resetAt;
}
