package com.ole.turapp.dto;

import java.time.Instant;

public record TrackPointData(
        Double latitude,
        Double longitude,
        Instant recordedAt,
        Double altitude
) {}