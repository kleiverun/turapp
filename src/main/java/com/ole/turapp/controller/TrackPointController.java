package com.ole.turapp.controller;

import com.ole.turapp.dto.TrackPointCreateRequest;
import com.ole.turapp.dto.TrackPointListResponse;
import com.ole.turapp.service.TrackPointService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TrackPointController {

    private final TrackPointService trackPointService;

    public TrackPointController(TrackPointService trackPointService) {
        this.trackPointService = trackPointService;
    }

    @PostMapping("/trackpoints")
    public ResponseEntity<TrackPointListResponse> addPoints(
            @RequestBody TrackPointCreateRequest request) {
        TrackPointListResponse response = trackPointService.addPoints(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/trips/{tripId}/trackpoints")
    public TrackPointListResponse getPoints(@PathVariable Long tripId) {
        return trackPointService.getPointsForTrip(tripId);
    }
}
