package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneRoleRequest {

    @NotBlank(message = "New role name is required")
    @Size(min = 2, max = 100, message = "Role name must be between 2 and 100 characters")
    private String newRoleName;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String newRoleDescription;
}
