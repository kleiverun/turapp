package com.ole.turapp.dto;

import java.util.List;

/**
 * A route with the full point line included. Used when the client needs to
 * draw all routes on the map in a single request, instead of fetching the
 * points route by route. {@code source} distinguishes planned routes from
 * GPX background trail networks.
 */
public record RouteWithPointsResponse(
        Long id,
        String name,
        String source,
        List<RoutePointData> points
) {}
