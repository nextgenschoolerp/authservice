package com.kvs.auth.dto;

/** The outcome for one account: CREATED (with the one-time temporary password) or FAILED (with the reason). */
public record StaffAccountResult(String employeeCode, String status, String userId, String username, String roleCode, String temporaryPassword, String message) { }
