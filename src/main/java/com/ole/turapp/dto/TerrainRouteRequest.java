package com.ole.turapp.dto;

public record TerrainRouteRequest(
        double fromLat,
        double fromLon,
        double toLat,
        double toLon
) {}
