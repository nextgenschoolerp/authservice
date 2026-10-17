package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SslCertificateResponse {

    private String tenantId;
    private String domainName;
    private boolean sslEnabled;
    private boolean autoRenewEnabled;
    private String tlsVersion;
    private Instant certificateExpiresAt;
    private String status;
}
