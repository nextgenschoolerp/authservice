package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchConcurrencyResponse {

    private String tenantId;
    private String branchCode;
    private String branchName;
    private long activeUsersCount;
    private int studentCapacity;
    private int staffCapacity;
    private int totalCapacity;
    private double utilizationPercentage;
    private boolean capacityExceeded;
    private String status;
}
