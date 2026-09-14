package com.ole.turapp.dto;

import java.util.List;

public record TerrainRouteResponse(
        List<PointDto> points,
        double distanceMeters,
        double ascentMeters,
        double descentMeters
) {
    public record PointDto(double latitude, double longitude) {}
}
