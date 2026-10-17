package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpValidationResponse {

    private String tenantId;
    private String ipAddress;
    private boolean allowed;
    private String reason;
}
