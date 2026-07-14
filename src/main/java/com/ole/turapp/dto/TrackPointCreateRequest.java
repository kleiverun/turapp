package com.ole.turapp.dto;

import java.util.List;

public record TrackPointCreateRequest(
        Long tripId,
        List<TrackPointData> points
) {
}
