package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleDelegationResponse {
    private Long id;
    private String tenantId;
    private String delegatorUserId;
    private String delegateeUserId;
    private Long roleId;
    private String roleName;
    private Instant startTime;
    private Instant endTime;
    private String reason;
    private String status;
}
