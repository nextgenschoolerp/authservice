package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpWhitelistRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotEmpty(message = "At least one IP subnet or address must be specified")
    private List<String> allowedSubnets;

    private boolean enforceCampusSubnet;
}
