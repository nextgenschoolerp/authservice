package com.kvs.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUnlockResponse {
    private String userId;
    private String username;
    private boolean unlocked;
    private String message;
    private Instant unlockedAt;
}
