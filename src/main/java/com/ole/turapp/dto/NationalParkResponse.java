package com.ole.turapp.dto;

import java.util.List;

public record NationalParkResponse(Long id, String name, List<NationalParkBoundaryPoint> boundary) {}
