package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpWhitelistResponse {

    private String tenantId;
    private List<String> allowedSubnets;
    private boolean enforceCampusSubnet;
    private String status;
}
