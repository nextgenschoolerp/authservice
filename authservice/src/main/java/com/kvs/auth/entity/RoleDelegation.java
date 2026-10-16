package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores temporary administrative authority delegations during executive leave windows.
 * Covers: US-00006 — Delegate administrative authority temporarily during executive leave windows
 * Actors: Department Head
 */
@Entity
@Table(name = "role_delegations")
@Getter
@Setter
public class RoleDelegation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "delegator_user_id", nullable = false, length = 100)
    private String delegatorUserId;

    @Column(name = "delegatee_user_id", nullable = false, length = 100)
    private String delegateeUserId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";
}
