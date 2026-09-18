package com.ole.turapp.service;

import com.ole.turapp.dto.TrackPointCreateRequest;
import com.ole.turapp.dto.TrackPointData;
import com.ole.turapp.dto.TrackPointListResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Role;
import com.ole.turapp.model.TrackPoint;
import com.ole.turapp.model.Trip;
import com.ole.turapp.model.User;
import com.ole.turapp.repository.TrackPointRepository;
import com.ole.turapp.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackPointServiceTest {

    @Mock
    private TrackPointRepository trackPointRepository;

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TrackPointService trackPointService;

    private Trip existingTrip(Long id) {
        User user = new User("hiker@example.com", "hash", "Hiker", Role.USER);
        Trip trip = new Trip("Test trip", user);
        trip.setId(id);
        return trip;
    }

    @Test
    void addPoints_Success_SavesAndReturnsPoints() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(existingTrip(10L)));

        TrackPointData data = new TrackPointData(59.9, 10.7, Instant.now(), 120.0);
        TrackPointCreateRequest request = new TrackPointCreateRequest(10L, List.of(data));

        when(trackPointRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        TrackPointListResponse response = trackPointService.addPoints(request);

        assertThat(response.tripId()).isEqualTo(10L);
        assertThat(response.points()).hasSize(1);
        assertThat(response.points().get(0).latitude()).isEqualTo(59.9);
    }

    @Test
    void addPoints_TripDoesNotExist_ThrowsException() {
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());
        TrackPointCreateRequest request = new TrackPointCreateRequest(10L,
                List.of(new TrackPointData(59.9, 10.7, Instant.now(), null)));

        assertThatThrownBy(() -> trackPointService.addPoints(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find trip with id 10");
    }

    @Test
    void addPoints_EmptyPointsList_ThrowsException() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(existingTrip(10L)));
        TrackPointCreateRequest request = new TrackPointCreateRequest(10L, List.of());

        assertThatThrownBy(() -> trackPointService.addPoints(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request must contain at least one trackpoint");
    }

    @Test
    void addPoints_MissingLatitude_ThrowsException() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(existingTrip(10L)));
        TrackPointData invalidData = new TrackPointData(null, 10.7, Instant.now(), null);
        TrackPointCreateRequest request = new TrackPointCreateRequest(10L, List.of(invalidData));

        assertThatThrownBy(() -> trackPointService.addPoints(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trackpoint is missing a required field");
    }

    @Test
    void getPointsForTrip_ReturnsMappedPoints() {
        TrackPoint point = new TrackPoint(existingTrip(10L), 59.9, 10.7, Instant.now());
        when(trackPointRepository.findByTripIdOrderByTimestampAsc(10L)).thenReturn(List.of(point));

        TrackPointListResponse response = trackPointService.getPointsForTrip(10L);

        assertThat(response.tripId()).isEqualTo(10L);
        assertThat(response.points()).hasSize(1);
    }

    @Test
    void getPointsForTrip_NoPoints_ReturnsEmptyList() {
        when(trackPointRepository.findByTripIdOrderByTimestampAsc(10L)).thenReturn(List.of());

        TrackPointListResponse response = trackPointService.getPointsForTrip(10L);

        assertThat(response.points()).isEmpty();
    }
}
