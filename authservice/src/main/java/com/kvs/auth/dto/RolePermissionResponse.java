package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionResponse {
    private Long id;
    private String screenName;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canExport;
}
