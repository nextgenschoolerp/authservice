package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Confirmation payload returned upon successful system bootstrap.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapResponse {

    private String tenantId;
    private String societyCode;
    private String societyName;
    private String branchCode;
    private String branchName;
    private String userId;
    private String username;
    private String email;
    private String temporaryPassword;
    private boolean mustChangePassword;
    private String message;
}
