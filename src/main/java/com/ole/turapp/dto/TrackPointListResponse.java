package com.ole.turapp.dto;

import java.util.List;

public record TrackPointListResponse(
        Long tripId,
        List<TrackPointData> points
) {
}
