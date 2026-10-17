package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores field-level data encryption configurations (e.g. Student Health Records).
 * Covers: US-00019, US-00039, US-00059, US-00079, US-00099
 * Actor: Principal / Compliance Officer
 */
@Entity
@Table(name = "field_encryption_policies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "entity_name"})
})
@Getter
@Setter
public class FieldEncryptionPolicy extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "encrypted_fields", nullable = false, length = 500)
    private String encryptedFields; // Comma-delimited list of field names

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm = "AES-256-GCM";

    @Column(name = "is_enforced", nullable = false)
    private boolean enforced = true;
}
