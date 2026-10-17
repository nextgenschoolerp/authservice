package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SslCertificateRequest {

    @NotBlank(message = "Domain name is required")
    private String domainName;

    private boolean sslEnabled;
    private boolean autoRenewEnabled;
    private String tlsVersion;
}
