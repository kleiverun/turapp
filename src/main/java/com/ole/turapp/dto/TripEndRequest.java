package com.ole.turapp.dto;

public record TripEndRequest(
        Double distanceMeters,
        Long durationSeconds
) {
}
