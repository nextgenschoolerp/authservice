package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** A user of a society with their roles and account state. Never carries credential data. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffMemberResponse {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String branchId;
    private String userType;
    /** Role codes from user_roles plus the user type, without the ROLE_ prefix. */
    private List<String> roles;
    private boolean active;
    private boolean locked;
    private Instant lockedUntil;
    private boolean mustChangePassword;
}
