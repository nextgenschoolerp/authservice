package com.kvs.auth.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDelegationRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Delegator User ID is required")
    private String delegatorUserId;

    @NotBlank(message = "Delegatee User ID is required")
    private String delegateeUserId;

    private Long roleId;

    @NotBlank(message = "Role name is required")
    private String roleName;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private Instant endTime;

    private String reason;
}
