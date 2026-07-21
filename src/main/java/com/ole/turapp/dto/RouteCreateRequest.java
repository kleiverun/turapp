package com.ole.turapp.dto;

import java.util.List;

/** JSON-basert ruteoppretting fra web-planleggeren (alternativ til GPX-import). */
public record RouteCreateRequest(String name, String description, List<PointData> points) {

    public record PointData(double latitude, double longitude) {
    }
}
