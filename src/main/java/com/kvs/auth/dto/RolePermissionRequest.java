package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionRequest {

    @NotBlank(message = "Screen name is required")
    @Size(max = 100)
    private String screenName;

    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canExport;
}
