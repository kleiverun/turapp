package com.ole.turapp.dto;

import java.util.List;

/**
 * Result of importing a GPX file: how many routes and points were created,
 * plus a summary of each imported route.
 */
public record GpxImportResponse(
        int routeCount,
        long pointCount,
        List<RouteResponse> routes
) {}
