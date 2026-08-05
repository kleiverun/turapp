package com.ole.turapp.dto;

import java.util.List;

public record NatureReserveResponse(Long id, String name, List<NatureReserveBoundaryPoint> boundary) {}
