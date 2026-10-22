package com.kvs.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolProfileRequest {

    @NotBlank(message = "School name is required")
    @Size(max = 150)
    private String schoolName;

    @NotBlank(message = "Campus name is required")
    @Size(max = 150)
    private String campusName;

    @NotBlank(message = "School email is required")
    @Email(message = "Enter a valid school email")
    @Size(max = 150)
    private String schoolEmail;

    @NotBlank(message = "School phone is required")
    @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "Enter a valid phone number")
    private String schoolPhone;

    @Size(max = 50)
    private String schoolReference;

    @Size(max = 150)
    private String website;

    @NotBlank(message = "School address is required")
    @Size(max = 300)
    private String address;
}
