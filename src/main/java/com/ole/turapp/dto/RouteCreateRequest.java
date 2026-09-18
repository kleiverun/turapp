package com.ole.turapp.dto;

import java.util.List;

/** JSON-based route creation from the web planner (an alternative to GPX import). */
public record RouteCreateRequest(String name, String description, List<PointData> points) {

    public record PointData(double latitude, double longitude) {
    }
}
