package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldEncryptionPolicyResponse {

    private String tenantId;
    private String entityName;
    private List<String> encryptedFields;
    private String algorithm;
    private boolean enforced;
    private String status;
}
