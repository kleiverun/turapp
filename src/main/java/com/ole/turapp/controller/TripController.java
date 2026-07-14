package com.ole.turapp.controller;

import com.ole.turapp.dto.TripCreateRequest;
import com.ole.turapp.dto.TripEndRequest;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.dto.TripUpdateRequest;
import com.ole.turapp.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @PathVariable Long userId,
            @RequestBody TripCreateRequest request) {
        TripResponse response = tripService.createTrip(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<TripResponse> getTrips(@PathVariable Long userId) {
        return tripService.getTripsForUser(userId);
    }

    @GetMapping("/{tripId}")
    public TripResponse getTrip(@PathVariable Long userId, @PathVariable Long tripId) {
        return tripService.getTrip(tripId);
    }

    @PatchMapping("/{tripId}")
    public TripResponse updateTrip(
            @PathVariable Long userId,
            @PathVariable Long tripId,
            @RequestBody TripUpdateRequest request) {
        return tripService.updateTrip(tripId, request);
    }

    @PostMapping("/{tripId}/end")
    public TripResponse endTrip(
            @PathVariable Long userId,
            @PathVariable Long tripId,
            @RequestBody TripEndRequest request) {
        return tripService.endTrip(tripId, request);
    }
}
