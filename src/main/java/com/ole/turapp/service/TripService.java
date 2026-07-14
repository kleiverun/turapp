package com.ole.turapp.service;

import com.ole.turapp.dto.TripCreateRequest;
import com.ole.turapp.dto.TripEndRequest;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.dto.TripUpdateRequest;
import com.ole.turapp.model.Trip;
import com.ole.turapp.model.User;
import com.ole.turapp.model.Visibility;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.repository.TripRepository;
import com.ole.turapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public TripService(TripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    public TripResponse createTrip(Long userId, TripCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Fant ikke bruker med id " + userId));

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setName(request.name());
        trip.setNotes(request.notes());
        Visibility visibility = parseVisibility(request.visibility());
        if (visibility != null) {
            trip.setVisibility(visibility);
        }
        trip.setStartedAt(Instant.now());

        Trip savedTrip = tripRepository.save(trip);

        return toResponse(savedTrip);
    }

    public List<TripResponse> getTripsForUser(Long userId) {
        return tripRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TripResponse getTrip(Long tripId) {
        return toResponse(findTrip(tripId));
    }

    public TripResponse updateTrip(Long tripId, TripUpdateRequest request) {
        Trip trip = findTrip(tripId);
        if (request.name() != null && !request.name().isBlank()) {
            trip.setName(request.name());
        }
        if (request.notes() != null) {
            trip.setNotes(request.notes());
        }
        return toResponse(tripRepository.save(trip));
    }

    public TripResponse endTrip(Long tripId, TripEndRequest request) {
        Trip trip = findTrip(tripId);
        if (trip.getEndedAt() != null) {
            throw new IllegalArgumentException("Turen er allerede avsluttet");
        }
        if (request.distanceMeters() != null) {
            if (request.distanceMeters() < 0) {
                throw new IllegalArgumentException("distanceMeters kan ikke være negativ");
            }
            trip.setDistanceMeters(request.distanceMeters());
        }
        if (request.durationSeconds() != null) {
            if (request.durationSeconds() < 0) {
                throw new IllegalArgumentException("durationSeconds kan ikke være negativ");
            }
            trip.setDurationSeconds(request.durationSeconds());
        }
        trip.setEndedAt(Instant.now());
        return toResponse(tripRepository.save(trip));
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Fant ikke tur med id " + tripId));
    }

    private Visibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Visibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Ugyldig visibility '" + value + "'. Tillatte verdier: " + Arrays.toString(Visibility.values()));
        }
    }

    private TripResponse toResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                trip.getNotes(),
                trip.getDistanceMeters(),
                trip.getDurationSeconds(),
                trip.getVisibility() != null ? trip.getVisibility().name() : null,
                trip.getStartedAt(),
                trip.getEndedAt()
        );
    }
}