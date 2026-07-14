package com.ole.turapp.dto;

import java.util.List;

/**
 * En rute med hele punktlinja inkludert. Brukes når klienten skal tegne
 * alle rutene på kartet i én forespørsel, i stedet for å hente punktene
 * rute for rute.
 */
public record RouteWithPointsResponse(
        Long id,
        String name,
        List<RoutePointData> points
) {}
