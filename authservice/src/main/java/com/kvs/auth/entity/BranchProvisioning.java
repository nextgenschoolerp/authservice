package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores branch onboarding and isolated database schema provisioning details.
 * Covers: US-00009 — Provision isolated database schemas for newly onboarded school branches
 * Actors: Super Admin
 */
@Entity
@Table(name = "branches_provisioning", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "branch_code"})
})
@Getter
@Setter
public class BranchProvisioning extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "branch_code", nullable = false, length = 50)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(name = "affiliation_board", nullable = false, length = 100)
    private String affiliationBoard;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "student_capacity", nullable = false)
    private int studentCapacity;

    @Column(name = "staff_capacity", nullable = false)
    private int staffCapacity;

    @Column(name = "isolated_db_enabled", nullable = false)
    private boolean isolatedDbEnabled = true;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PROVISIONED";
}
