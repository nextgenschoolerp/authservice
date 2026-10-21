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
public class RegionalSettingsResponse {
    private String tenantId;
    private String timeZone;
    private String currency;
    private String dateFormat;
    private String language;
    /** False while the tenant has never saved its preferences (the values are the defaults). */
    private boolean saved;
    private Instant updatedAt;
    private String updatedBy;
}
