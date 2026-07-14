package com.ole.turapp.dto;

import java.time.Instant;

/**
 * Summary of a route without its points. {@code pointCount} tells how many
 * RoutePoints make up the line, so a client can decide whether to load them.
 */
public record RouteResponse(
        Long id,
        String name,
        String description,
        String visibility,
        Instant createdAt,
        int pointCount
) {}
