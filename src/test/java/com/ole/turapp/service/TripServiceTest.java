package com.ole.turapp.service;

import com.ole.turapp.dto.TripCreateRequest;
import com.ole.turapp.dto.TripEndRequest;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.dto.TripUpdateRequest;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Role;
import com.ole.turapp.model.Trip;
import com.ole.turapp.model.User;
import com.ole.turapp.repository.TrackPointRepository;
import com.ole.turapp.repository.TripRepository;
import com.ole.turapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TrackPointRepository trackPointRepository;

    @InjectMocks
    private TripService tripService;

    private TripCreateRequest validRequest(String name, String notes) {
        return new TripCreateRequest(name, notes, "PRIVATE");
    }

    @Test
    void testCreateTrip_UserDoesNotExist_ThrowsException() {
        // Arrange
        TripCreateRequest request = validRequest("Testtrip", "Notes of a test");
        Long testId = 900L;
        when(userRepository.findById(testId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tripService.createTrip(testId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find user");
    }

    @Test
    void testCreateTrip_Success() {
        // Arrange
        Long userId = 1L;
        User existingUser = new User("email@gmail.com", "hashedPassword", "user1", Role.USER);
        TripCreateRequest request = validRequest("Testtrip", "Notes of a test");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip tripArg = invocation.getArgument(0);
            tripArg.setId(1L);
            return tripArg;
        });

        // Act
        TripResponse response = tripService.createTrip(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isNotNull();
    }

    @Test
    void testGetTripsForUser_Success() {
        // Arrange
        Long userId = 1L;
        User existingUser = new User("email@gmail.com", "hashedPassword", "user1", Role.USER);
        ReflectionTestUtils.setField(existingUser, "id", userId);

        Trip existingTrip = new Trip();
        existingTrip.setName("TestTrip");
        existingTrip.setUser(existingUser);

        when(tripRepository.findByUserId(userId)).thenReturn(List.of(existingTrip));

        // Act
        List<TripResponse> allUsersTrips = tripService.getTripsForUser(userId);

        // Assert
        assertThat(allUsersTrips)
                .hasSize(1)
                .extracting(TripResponse::name)
                .containsExactly("TestTrip");
    }
    @Test
    void testGetTrip_Success() {
        // Arrange
        Long tripId = 1L;
        User existingUser = new User("email@gmail.com", "hashedPassword", "user1", Role.USER);

        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setName("TestTrip");
        existingTrip.setUser(existingUser);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));

        // Act
        TripResponse response = tripService.getTrip(tripId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("TestTrip");
    }

    @Test
    void testGetTrip_DoesNotExist_ThrowsException() {
        // Arrange
        Long tripId = 900L;
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tripService.getTrip(tripId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find a trip");
    }

    @Test
    void testCreateTrip_InvalidVisibility_ThrowsException() {
        // Arrange
        Long userId = 1L;
        User existingUser = new User("email@gmail.com", "hashedPassword", "user1", Role.USER);
        TripCreateRequest request = new TripCreateRequest("Testtrip", "Notes", "NOT_A_VISIBILITY");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> tripService.createTrip(userId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testUpdateTrip_Success() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setName("OldName");
        existingTrip.setNotes("OldNotes");

        TripUpdateRequest request = new TripUpdateRequest("NewName", "NewNotes");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TripResponse response = tripService.updateTrip(tripId, request);

        // Assert
        assertThat(response.name()).isEqualTo("NewName");
        assertThat(response.notes()).isEqualTo("NewNotes");
    }

    @Test
    void testUpdateTrip_BlankName_KeepsExistingName() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setName("OldName");
        existingTrip.setNotes("OldNotes");

        TripUpdateRequest request = new TripUpdateRequest("  ", null);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TripResponse response = tripService.updateTrip(tripId, request);

        // Assert
        assertThat(response.name()).isEqualTo("OldName");
        assertThat(response.notes()).isEqualTo("OldNotes");
    }

    @Test
    void testUpdateTrip_DoesNotExist_ThrowsException() {
        // Arrange
        Long tripId = 900L;
        TripUpdateRequest request = new TripUpdateRequest("NewName", "NewNotes");
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tripService.updateTrip(tripId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find a trip");
    }

    @Test
    void testEndTrip_Success() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setName("TestTrip");

        TripEndRequest request = new TripEndRequest(5000.0, 3600L);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TripResponse response = tripService.endTrip(tripId, request);

        // Assert
        assertThat(response.distanceMeters()).isEqualTo(5000.0);
        assertThat(response.durationSeconds()).isEqualTo(3600L);
        assertThat(response.endedAt()).isNotNull();
    }

    @Test
    void testEndTrip_AlreadyEnded_ThrowsException() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);
        existingTrip.setEndedAt(java.time.Instant.now());

        TripEndRequest request = new TripEndRequest(5000.0, 3600L);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));

        // Act & Assert
        assertThatThrownBy(() -> tripService.endTrip(tripId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Trip has already ended");
    }

    @Test
    void testEndTrip_NegativeDistance_ThrowsException() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);

        TripEndRequest request = new TripEndRequest(-1.0, 3600L);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));

        // Act & Assert
        assertThatThrownBy(() -> tripService.endTrip(tripId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("distanceMeters cannot be negative");
    }

    @Test
    void testEndTrip_NegativeDuration_ThrowsException() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);

        TripEndRequest request = new TripEndRequest(5000.0, -1L);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));

        // Act & Assert
        assertThatThrownBy(() -> tripService.endTrip(tripId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("durationSeconds cannot be negative");
    }

    @Test
    void testEndTrip_DoesNotExist_ThrowsException() {
        // Arrange
        Long tripId = 900L;
        TripEndRequest request = new TripEndRequest(5000.0, 3600L);
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tripService.endTrip(tripId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find a trip");
    }

    @Test
    void testDeleteTrip_Success() {
        // Arrange
        Long tripId = 1L;
        Trip existingTrip = new Trip();
        existingTrip.setId(tripId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));

        // Act
        tripService.deleteTrip(tripId);

        // Assert
        verify(trackPointRepository).deleteByTripId(tripId);
        verify(tripRepository).delete(existingTrip);
    }

    @Test
    void testDeleteTrip_DoesNotExist_ThrowsException() {
        // Arrange
        Long tripId = 900L;
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tripService.deleteTrip(tripId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find a trip");

        verify(trackPointRepository, never()).deleteByTripId(any());
        verify(tripRepository, never()).delete(any());
    }
}