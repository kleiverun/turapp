package com.ole.turapp.service;

import com.ole.turapp.dto.GpxImportResponse;
import com.ole.turapp.dto.RoutePointData;
import com.ole.turapp.dto.RoutePointListResponse;
import com.ole.turapp.dto.RouteResponse;
import com.ole.turapp.dto.RouteWithPointsResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Route;
import com.ole.turapp.model.RoutePoint;
import com.ole.turapp.model.User;
import com.ole.turapp.repository.RoutePointRepository;
import com.ole.turapp.repository.RouteRepository;
import com.ole.turapp.repository.UserRepository;
import com.ole.turapp.service.GpxRouteParser.Coordinate;
import com.ole.turapp.service.GpxRouteParser.ParsedRoute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final RoutePointRepository routePointRepository;
    private final UserRepository userRepository;
    private final GpxRouteParser gpxRouteParser;

    public RouteService(RouteRepository routeRepository,
                        RoutePointRepository routePointRepository,
                        UserRepository userRepository,
                        GpxRouteParser gpxRouteParser) {
        this.routeRepository = routeRepository;
        this.routePointRepository = routePointRepository;
        this.userRepository = userRepository;
        this.gpxRouteParser = gpxRouteParser;
    }

    /**
     * Imports every {@code <rte>} from a GPX stream as a route owned by the given user.
     * Routes without any points are skipped.
     */
    @Transactional
    public GpxImportResponse importGpx(Long userId, InputStream gpx) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Fant ikke bruker med id " + userId));

        List<ParsedRoute> parsedRoutes = parse(gpx);
        if (parsedRoutes.isEmpty()) {
            throw new IllegalArgumentException("GPX-filen inneholder ingen ruter (<rte>)");
        }

        List<RouteResponse> imported = new ArrayList<>();
        long totalPoints = 0;

        for (ParsedRoute parsed : parsedRoutes) {
            if (parsed.points().isEmpty()) {
                continue;
            }

            Route route = new Route(routeName(parsed), user);
            route.setDescription(parsed.description());
            Route savedRoute = routeRepository.save(route);

            List<RoutePoint> points = new ArrayList<>(parsed.points().size());
            int order = 0;
            for (Coordinate coord : parsed.points()) {
                RoutePoint point = new RoutePoint(order++, coord.latitude(), coord.longitude());
                point.setRoute(savedRoute);
                points.add(point);
            }
            routePointRepository.saveAll(points);

            totalPoints += points.size();
            imported.add(toResponse(savedRoute, points.size()));
        }

        if (imported.isEmpty()) {
            throw new IllegalArgumentException("GPX-filen inneholder ruter, men ingen av dem har rutepunkter");
        }

        return new GpxImportResponse(imported.size(), totalPoints, imported);
    }

    /**
     * Alle brukerens ruter med punktene inkludert, klare til å tegnes på kartet.
     * Punktene hentes i én spørring og grupperes per rute.
     */
    public List<RouteWithPointsResponse> getRoutesWithPointsForUser(Long userId) {
        Map<Long, Route> routesById = new LinkedHashMap<>();
        for (Route route : routeRepository.findByUserId(userId)) {
            routesById.put(route.getId(), route);
        }

        Map<Long, List<RoutePointData>> pointsByRouteId = new LinkedHashMap<>();
        for (RoutePoint point : routePointRepository.findByRouteUserIdOrderByRouteIdAscPointOrderAsc(userId)) {
            pointsByRouteId
                    .computeIfAbsent(point.getRoute().getId(), id -> new ArrayList<>())
                    .add(new RoutePointData(point.getPointOrder(), point.getLatitude(), point.getLongitude()));
        }

        return routesById.values().stream()
                .map(route -> new RouteWithPointsResponse(
                        route.getId(),
                        route.getName(),
                        pointsByRouteId.getOrDefault(route.getId(), List.of())))
                .toList();
    }

    public List<RouteResponse> getRoutesForUser(Long userId) {
        return routeRepository.findByUserId(userId).stream()
                .map(route -> toResponse(route, routePointRepository.countByRouteId(route.getId())))
                .toList();
    }

    public RoutePointListResponse getPointsForRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new NotFoundException("Fant ikke rute med id " + routeId);
        }
        List<RoutePointData> points = routePointRepository.findByRouteIdOrderByPointOrderAsc(routeId).stream()
                .map(p -> new RoutePointData(p.getPointOrder(), p.getLatitude(), p.getLongitude()))
                .toList();
        return new RoutePointListResponse(routeId, points);
    }

    private List<ParsedRoute> parse(InputStream gpx) {
        try (InputStream in = gpx) {
            return gpxRouteParser.parse(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Kunne ikke lese GPX-filen: " + e.getMessage(), e);
        }
    }

    /** GPX route names are optional; fall back to a placeholder so the DB column stays non-null. */
    private String routeName(ParsedRoute parsed) {
        return (parsed.name() != null && !parsed.name().isBlank()) ? parsed.name() : "Uten navn";
    }

    private RouteResponse toResponse(Route route, int pointCount) {
        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getVisibility() != null ? route.getVisibility().name() : null,
                route.getCreatedAt(),
                pointCount
        );
    }
}
