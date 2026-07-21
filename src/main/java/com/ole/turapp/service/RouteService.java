package com.ole.turapp.service;

import com.ole.turapp.dto.GpxImportResponse;
import com.ole.turapp.dto.RouteCreateRequest;
import com.ole.turapp.dto.RoutePointData;
import com.ole.turapp.dto.RouteUpdateRequest;
import com.ole.turapp.dto.RoutePointListResponse;
import com.ole.turapp.dto.RouteResponse;
import com.ole.turapp.dto.RouteWithPointsResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Route;
import com.ole.turapp.model.RoutePoint;
import com.ole.turapp.model.RouteSource;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // Signaturer for brukerens eksisterende GPX-ruter, slik at samme fil
        // kan importeres flere ganger uten å lage duplikater.
        Set<String> existing = existingGpxSignatures(userId);

        List<RouteResponse> imported = new ArrayList<>();
        long totalPoints = 0;

        for (ParsedRoute parsed : parsedRoutes) {
            if (parsed.points().isEmpty()) {
                continue;
            }
            if (!existing.add(gpxSignature(routeName(parsed), parsed.points()))) {
                continue; // identisk rute finnes allerede
            }

            Route route = new Route(routeName(parsed), user);
            route.setDescription(parsed.description());
            route.setSource(RouteSource.GPX_IMPORT);
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
            throw new IllegalArgumentException(
                    "Ingen nye ruter å importere — alle rutene i filen finnes allerede (eller mangler punkter)");
        }

        return new GpxImportResponse(imported.size(), totalPoints, imported);
    }

    /** Signaturer (navn + antall punkter + start/slutt) for brukerens GPX-ruter. */
    private Set<String> existingGpxSignatures(Long userId) {
        Map<Long, List<RoutePoint>> pointsByRoute = new LinkedHashMap<>();
        for (RoutePoint p : routePointRepository.findByRouteUserIdOrderByRouteIdAscPointOrderAsc(userId)) {
            pointsByRoute.computeIfAbsent(p.getRoute().getId(), id -> new ArrayList<>()).add(p);
        }
        Set<String> signatures = new HashSet<>();
        for (Route route : routeRepository.findByUserId(userId)) {
            if (route.getSource() != RouteSource.GPX_IMPORT) continue;
            List<RoutePoint> pts = pointsByRoute.getOrDefault(route.getId(), List.of());
            if (pts.isEmpty()) continue;
            RoutePoint first = pts.get(0);
            RoutePoint last = pts.get(pts.size() - 1);
            signatures.add(signature(route.getName(), pts.size(),
                    first.getLatitude(), first.getLongitude(), last.getLatitude(), last.getLongitude()));
        }
        return signatures;
    }

    private String gpxSignature(String name, List<Coordinate> points) {
        Coordinate first = points.get(0);
        Coordinate last = points.get(points.size() - 1);
        return signature(name, points.size(),
                first.latitude(), first.longitude(), last.latitude(), last.longitude());
    }

    private String signature(String name, int pointCount,
                             double firstLat, double firstLng, double lastLat, double lastLng) {
        return name + '|' + pointCount + '|'
                + String.format(java.util.Locale.ROOT, "%.6f,%.6f,%.6f,%.6f",
                        firstLat, firstLng, lastLat, lastLng);
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
                        route.getSource() != null ? route.getSource().name() : RouteSource.PLANNED.name(),
                        pointsByRouteId.getOrDefault(route.getId(), List.of())))
                .toList();
    }

    /** Oppretter en rute fra JSON (web-planleggeren) med punktene i innsendt rekkefølge. */
    @Transactional
    public RouteResponse createRoute(Long userId, RouteCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Fant ikke bruker med id " + userId));

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Rutenavn er påkrevd");
        }
        if (request.points() == null || request.points().size() < 2) {
            throw new IllegalArgumentException("En rute må ha minst 2 punkter");
        }

        Route route = new Route(request.name().trim(), user);
        route.setDescription(request.description());
        Route savedRoute = routeRepository.save(route);

        List<RoutePoint> points = new ArrayList<>(request.points().size());
        int order = 0;
        for (RouteCreateRequest.PointData p : request.points()) {
            RoutePoint point = new RoutePoint(order++, p.latitude(), p.longitude());
            point.setRoute(savedRoute);
            points.add(point);
        }
        routePointRepository.saveAll(points);

        return toResponse(savedRoute, points.size());
    }

    @Transactional
    public RouteResponse updateRoute(Long userId, Long routeId, RouteUpdateRequest request) {
        Route route = requireOwnedRoute(userId, routeId);
        if (request.name() != null && !request.name().isBlank()) {
            route.setName(request.name().trim());
        }
        route.setDescription(request.description());
        Route saved = routeRepository.save(route);
        return toResponse(saved, routePointRepository.countByRouteId(routeId));
    }

    @Transactional
    public void deleteRoute(Long userId, Long routeId) {
        Route route = requireOwnedRoute(userId, routeId);
        routePointRepository.deleteByRouteId(routeId);
        routeRepository.delete(route);
    }

    private Route requireOwnedRoute(Long userId, Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new NotFoundException("Fant ikke rute med id " + routeId));
        if (!route.getUser().getId().equals(userId)) {
            throw new NotFoundException("Fant ikke rute med id " + routeId);
        }
        return route;
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
                pointCount,
                route.getSource() != null ? route.getSource().name() : RouteSource.PLANNED.name()
        );
    }
}
