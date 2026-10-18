package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** A society (tenant) with its campuses, for directory listings. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocietyDirectoryResponse {
    private String tenantId;
    private String societyCode;
    private String societyName;
    private String domainName;
    private String status;
    private List<Campus> campuses;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Campus {
        private String branchCode;
        private String branchName;
        private String affiliationBoard;
        private String status;
    }
}
