package com.ole.turapp.dto;

import java.time.Instant;

public record LoginResponse(
        Long id,
        String email,
        String displayName,
        String role,
        Instant createdAt,
        String token
) {}
