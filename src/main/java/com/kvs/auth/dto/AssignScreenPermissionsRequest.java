package com.kvs.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignScreenPermissionsRequest {

    @NotEmpty(message = "Permissions list cannot be empty")
    @Valid
    private List<RolePermissionRequest> permissions;
}
