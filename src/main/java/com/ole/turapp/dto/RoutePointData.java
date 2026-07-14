package com.ole.turapp.dto;

/**
 * A single coordinate on a route, in order. WGS84 decimal degrees.
 */
public record RoutePointData(
        int pointOrder,
        double latitude,
        double longitude
) {}
