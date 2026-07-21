package com.ole.turapp.service;

import com.ole.turapp.dto.TrackPointCreateRequest;
import com.ole.turapp.dto.TrackPointData;
import com.ole.turapp.dto.TrackPointListResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.TrackPoint;
import com.ole.turapp.model.Trip;
import com.ole.turapp.repository.TrackPointRepository;
import com.ole.turapp.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrackPointService {

    private final TrackPointRepository trackPointRepository;
    private final TripRepository tripRepository;

    public TrackPointService(TrackPointRepository trackPointRepository, TripRepository tripRepository) {
        this.trackPointRepository = trackPointRepository;
        this.tripRepository = tripRepository;
    }

    public TrackPointListResponse addPoints(TrackPointCreateRequest request) {
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new NotFoundException("Fant ikke tur med id " + request.tripId()));

        if (request.points() == null || request.points().isEmpty()) {
            throw new IllegalArgumentException("Forespørselen må inneholde minst ett trackpoint");
        }

        List<TrackPoint> pointsToSave = request.points().stream()
                .map(data -> toEntity(data, trip))
                .toList();

        List<TrackPoint> savedPoints = trackPointRepository.saveAll(pointsToSave);

        List<TrackPointData> responseData = savedPoints.stream()
                .map(this::toData)
                .toList();

        return new TrackPointListResponse(trip.getId(), responseData);
    }

    public TrackPointListResponse getPointsForTrip(Long tripId) {
        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimestampAsc(tripId);

        List<TrackPointData> responseData = points.stream()
                .map(this::toData)
                .toList();

        return new TrackPointListResponse(tripId, responseData);
    }

    private TrackPoint toEntity(TrackPointData data, Trip trip) {
        if (data.latitude() == null || data.longitude() == null || data.recordedAt() == null) {
            throw new IllegalArgumentException(
                    "Trackpoint mangler påkrevd felt: latitude, longitude og recordedAt kan ikke være null");
        }
        TrackPoint point = new TrackPoint(
                trip,
                data.latitude(),
                data.longitude(),
                data.recordedAt()
        );
        point.setAltitude(data.altitude());
        return point;
    }

    private TrackPointData toData(TrackPoint point) {
        return new TrackPointData(
                point.getLatitude(),
                point.getLongitude(),
                point.getTimestamp(),
                point.getAltitude()
        );
    }
}