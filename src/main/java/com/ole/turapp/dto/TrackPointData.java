package com.ole.turapp.dto;

import java.time.Instant;

public record TrackPointData(
        Double latitude,
        Double longitude,
       //Commented out for simplicity in this interation: 12.07.26
        // Double altitudeMeters,
       // Double accuracyMeters,
        Instant recordedAt
) {}