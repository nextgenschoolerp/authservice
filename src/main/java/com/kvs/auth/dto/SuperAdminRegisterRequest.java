package com.kvs.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for onboarding a new educational society/trust and registering its root Super Admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminRegisterRequest {

    // --- Educational Society / Tenant Info ---
    @NotBlank(message = "Society code is required")
    @Size(min = 2, max = 50, message = "Society code must be between 2 and 50 characters")
    private String societyCode;

    @NotBlank(message = "Society name is required")
    @Size(max = 255, message = "Society name must not exceed 255 characters")
    private String societyName;

    private String domainName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be a valid email format")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;

    // --- Super Admin User Info ---
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    // --- Optional Campus / Plan Defaults ---
    private String branchCode;
    private String branchName;
    private String affiliationBoard;
    private String planTier;
}
