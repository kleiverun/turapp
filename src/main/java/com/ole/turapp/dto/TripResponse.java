package com.ole.turapp.dto;

import java.time.Instant;

public record TripResponse(
        Long id,
        String name,
        String notes,
        Double distanceMeters,
        Long durationSeconds,
        String visibility,
        Instant startedAt,
        Instant endedAt
) {
}
