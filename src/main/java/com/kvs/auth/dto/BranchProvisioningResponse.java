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
public class BranchProvisioningResponse {
    private Long id;
    private String tenantId;
    private String branchCode;
    private String branchName;
    private String affiliationBoard;
    private String schemaName;
    private int studentCapacity;
    private int staffCapacity;
    private boolean isolatedDbEnabled;
    private String status;
    private Instant createdAt;
}
