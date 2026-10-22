package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * The school's profile (Settings → General → School profile): name, campus, contact details, address and crest,
 * used across the parent portal, documents and communications. One row per tenant.
 */
@Entity
@Table(name = "tenant_school_profiles")
@Getter
@Setter
public class TenantSchoolProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", unique = true, nullable = false, length = 100)
    private String tenantId;

    @Column(name = "school_name", nullable = false, length = 150)
    private String schoolName;

    @Column(name = "campus_name", nullable = false, length = 150)
    private String campusName;

    @Column(name = "school_email", nullable = false, length = 150)
    private String schoolEmail;

    @Column(name = "school_phone", nullable = false, length = 30)
    private String schoolPhone;

    @Column(name = "school_reference", length = 50)
    private String schoolReference;

    @Column(name = "website", length = 150)
    private String website;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    /** PNG, JPEG or SVG bytes; null until a crest is uploaded */
    @Column(name = "crest", columnDefinition = "bytea")
    private byte[] crest;

    @Column(name = "crest_content_type", length = 50)
    private String crestContentType;
}
