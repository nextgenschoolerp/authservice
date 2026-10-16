package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a custom role in the RBAC system.
 * Covers: US-00002 — Assign screen-level permissions to custom roles
 * Actors: Trustee
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique role name (e.g., PRINCIPAL, ADMIN_CLERK) */
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    /** Human-readable description of the role's purpose */
    @Column(name = "description", length = 255)
    private String description;

    /** Whether this role is currently active */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
