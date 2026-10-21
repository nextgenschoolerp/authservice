package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response payload returned upon successful Super Admin registration and society onboarding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminRegisterResponse {

    private String tenantId;
    private String societyCode;
    private String societyName;
    private String userId;
    private String username;
    private String email;
    private String userType;
    private List<String> roles;
    private String status;
    private String message;
    private Instant createdAt;
}
