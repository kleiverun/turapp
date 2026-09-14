package com.ole.turapp.controller;

import com.ole.turapp.dto.TerrainRouteRequest;
import com.ole.turapp.dto.TerrainRouteResponse;
import com.ole.turapp.service.TerrainRouteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
public class TerrainRouteController {

    private final TerrainRouteService terrainRouteService;

    public TerrainRouteController(TerrainRouteService terrainRouteService) {
        this.terrainRouteService = terrainRouteService;
    }

    @PostMapping("/terrain")
    public TerrainRouteResponse terrain(@RequestBody TerrainRouteRequest request) {
        return terrainRouteService.route(request);
    }
}
