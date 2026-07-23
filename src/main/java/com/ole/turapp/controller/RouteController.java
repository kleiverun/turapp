package com.ole.turapp.controller;

import com.ole.turapp.config.AuthUtils;
import com.ole.turapp.dto.GpxImportResponse;
import com.ole.turapp.dto.RouteCreateRequest;
import com.ole.turapp.dto.RoutePointListResponse;
import com.ole.turapp.dto.RouteResponse;
import com.ole.turapp.dto.RouteUpdateRequest;
import com.ole.turapp.dto.RouteWithPointsResponse;
import com.ole.turapp.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping(value = "/users/{userId}/routes/import", consumes = "multipart/form-data")
    public ResponseEntity<GpxImportResponse> importGpx(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        AuthUtils.requireOwner(userId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ingen fil ble lastet opp (feltet 'file' er tomt)");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.importGpx(userId, file.getInputStream()));
    }

    @PostMapping("/users/{userId}/routes")
    public ResponseEntity<RouteResponse> createRoute(
            @PathVariable Long userId,
            @RequestBody RouteCreateRequest request) {
        AuthUtils.requireOwner(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(userId, request));
    }

    @PatchMapping("/users/{userId}/routes/{routeId}")
    public RouteResponse updateRoute(
            @PathVariable Long userId,
            @PathVariable Long routeId,
            @RequestBody RouteUpdateRequest request) {
        AuthUtils.requireOwner(userId);
        return routeService.updateRoute(userId, routeId, request);
    }

    @DeleteMapping("/users/{userId}/routes/{routeId}")
    public ResponseEntity<Void> deleteRoute(
            @PathVariable Long userId,
            @PathVariable Long routeId) {
        AuthUtils.requireOwner(userId);
        routeService.deleteRoute(userId, routeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/routes")
    public List<RouteResponse> getRoutes(@PathVariable Long userId) {
        AuthUtils.requireOwner(userId);
        return routeService.getRoutesForUser(userId);
    }

    @GetMapping("/users/{userId}/routes/with-points")
    public List<RouteWithPointsResponse> getRoutesWithPoints(@PathVariable Long userId) {
        AuthUtils.requireOwner(userId);
        return routeService.getRoutesWithPointsForUser(userId);
    }

    @GetMapping("/routes/{routeId}/points")
    public RoutePointListResponse getRoutePoints(@PathVariable Long routeId) {
        return routeService.getPointsForRoute(routeId);
    }
}
