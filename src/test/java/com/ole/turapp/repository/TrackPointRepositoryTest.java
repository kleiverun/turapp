package com.ole.turapp.repository;

import com.ole.turapp.model.Role;
import com.ole.turapp.model.TrackPoint;
import com.ole.turapp.model.Trip;
import com.ole.turapp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class TrackPointRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TrackPointRepository trackPointRepository;

    @Test
    void findByTripIdOrderByTimestampAsc_ReturnsPointsInTimeOrder() {
        User user = entityManager.persistAndFlush(new User("hiker@example.com", "hash", "Hiker", Role.USER));
        Trip trip = entityManager.persistAndFlush(new Trip("Test trip", user));

        Instant now = Instant.now();
        entityManager.persistAndFlush(new TrackPoint(trip, 59.91, 10.71, now.plus(1, ChronoUnit.MINUTES)));
        entityManager.persistAndFlush(new TrackPoint(trip, 59.9, 10.7, now));

        List<TrackPoint> points = trackPointRepository.findByTripIdOrderByTimestampAsc(trip.getId());

        assertThat(points)
                .extracting(TrackPoint::getLatitude)
                .containsExactly(59.9, 59.91);
    }

    @Test
    void deleteByTripId_RemovesAllPointsForTrip() {
        User user = entityManager.persistAndFlush(new User("hiker@example.com", "hash", "Hiker", Role.USER));
        Trip trip = entityManager.persistAndFlush(new Trip("Test trip", user));
        entityManager.persistAndFlush(new TrackPoint(trip, 59.9, 10.7, Instant.now()));

        trackPointRepository.deleteByTripId(trip.getId());
        entityManager.flush();

        assertThat(trackPointRepository.findByTripIdOrderByTimestampAsc(trip.getId())).isEmpty();
    }
}
