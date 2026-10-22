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
public class SchoolProfileResponse {

    private String tenantId;
    private String schoolName;
    private String campusName;
    private String schoolEmail;
    private String schoolPhone;
    private String schoolReference;
    private String website;
    private String address;
    private boolean hasCrest;
    /** False when the profile was never saved and comes from the tenant record */
    private boolean saved;
    private Instant updatedAt;
}
