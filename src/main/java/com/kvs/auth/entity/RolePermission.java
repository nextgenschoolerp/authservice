package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Stores screen-level permission assignments for a role.
 * Covers: US-00002 — Assign screen-level Read/Write/Delete/Export permissions to custom roles
 * Actors: Trustee
 */
@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_id", "screen_name"})
})
@Getter
@Setter
public class RolePermission extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The role this permission belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** Screen/module identifier (e.g., STUDENT_PROFILE, FEE_MANAGEMENT) */
    @Column(name = "screen_name", nullable = false, length = 100)
    private String screenName;

    /** Permission to read/view records on this screen */
    @Column(name = "can_read", nullable = false)
    private boolean canRead = false;

    /** Permission to create/write records on this screen */
    @Column(name = "can_write", nullable = false)
    private boolean canWrite = false;

    /** Permission to delete records on this screen */
    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    /** Permission to export data from this screen */
    @Column(name = "can_export", nullable = false)
    private boolean canExport = false;
}
