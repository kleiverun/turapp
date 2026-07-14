package com.ole.turapp.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        String role,
        Instant createdAt
) {
}
