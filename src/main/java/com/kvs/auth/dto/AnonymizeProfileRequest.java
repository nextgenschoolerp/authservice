package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnonymizeProfileRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Profile type is required (e.g. STUDENT, STAFF)")
    private String profileType;

    @NotBlank(message = "Request type is required (e.g. RIGHT_TO_BE_FORGOTTEN, ANONYMIZATION)")
    private String requestType;

    private String fieldsToAnonymize; // Optional specific fields, e.g. "email,phone,address"

    @NotBlank(message = "Reason is required")
    private String reason;
}
