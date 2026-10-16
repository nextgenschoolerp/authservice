package com.kvs.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchProvisioningRequest {

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotBlank(message = "Affiliation board is required")
    private String affiliationBoard; // e.g. CBSE, ICSE, State Board

    @NotBlank(message = "Schema name is required")
    private String schemaName;

    @Min(value = 1, message = "Student capacity must be at least 1")
    private int studentCapacity;

    @Min(value = 1, message = "Staff capacity must be at least 1")
    private int staffCapacity;

    private boolean isolatedDbEnabled = true;
}
