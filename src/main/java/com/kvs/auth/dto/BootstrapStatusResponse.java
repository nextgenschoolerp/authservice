package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status response indicating whether the system requires initial setup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapStatusResponse {

    private boolean initialized;
    private boolean bootstrapRequired;
    private long tenantCount;
    private long userCount;
    private String systemVersion;
}
