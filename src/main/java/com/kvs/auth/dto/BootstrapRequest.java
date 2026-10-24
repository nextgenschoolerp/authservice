package com.kvs.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for initial zero-state system onboarding and IT Administrator provisioning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapRequest {

    // --- Organization / Educational Society Info ---
    @NotBlank(message = "Society / Group Name is required")
    @Size(max = 255, message = "Society name must not exceed 255 characters")
    private String societyName;

    @NotBlank(message = "Society Code is required")
    @Size(min = 2, max = 50, message = "Society code must be between 2 and 50 characters")
    private String societyCode;

    private String domainName;

    private String curriculumBoard;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be a valid email format")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;

    // --- Primary Campus / Branch Info ---
    @NotBlank(message = "Branch / Campus Name is required")
    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    private String branchName;

    @NotBlank(message = "Branch Code is required")
    @Size(min = 2, max = 50, message = "Branch code must be between 2 and 50 characters")
    private String branchCode;

    private String city;
    private String state;
    private String address;
    private String pincode;

    // --- Initial IT Administrator Info ---
    @NotBlank(message = "IT Admin Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String adminUsername;

    @NotBlank(message = "IT Admin First Name is required")
    private String adminFirstName;

    @NotBlank(message = "IT Admin Last Name is required")
    private String adminLastName;

    @NotBlank(message = "IT Admin Email is required")
    @Email(message = "IT Admin email must be a valid email format")
    private String adminEmail;

    private String adminPhone;

    // Optional: if not supplied, a secure temporary password will be auto-generated
    private String temporaryPassword;
}
