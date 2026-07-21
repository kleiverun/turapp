package com.ole.turapp.dto;

import java.util.List;

/**
 * En rute med hele punktlinja inkludert. Brukes når klienten skal tegne
 * alle rutene på kartet i én forespørsel, i stedet for å hente punktene
 * rute for rute. {@code source} skiller planlagte ruter fra GPX-bakgrunnsnett.
 */
public record RouteWithPointsResponse(
        Long id,
        String name,
        String source,
        List<RoutePointData> points
) {}
