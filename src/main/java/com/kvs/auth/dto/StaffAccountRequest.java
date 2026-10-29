package com.kvs.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Sign-in accounts to create for staff the HR import has just added (at most 500 per call). */
public record StaffAccountRequest(@NotEmpty @Size(max = 500) @Valid List<Account> accounts) {

    /** One staff member: the role name as written in the import file ("Subject Teacher") and the campus code of the staff record. */
    public record Account(String employeeCode, String firstName, String lastName, String email, String roleName, String branchId) { }
}
