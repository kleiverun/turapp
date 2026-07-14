package com.ole.turapp.dto;

import java.util.List;

public record RoutePointListResponse(
        Long routeId,
        List<RoutePointData> points
) {}
