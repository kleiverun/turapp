package com.ole.turapp.repository;

import com.ole.turapp.model.Role;
import com.ole.turapp.model.Trip;
import com.ole.turapp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class TripRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TripRepository tripRepository;

    @Test
    void findByUserId_ReturnsOnlyThatUsersTrips() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        User other = entityManager.persistAndFlush(new User("other@example.com", "hash", "Other", Role.USER));

        entityManager.persistAndFlush(new Trip("Owner trip 1", owner));
        entityManager.persistAndFlush(new Trip("Owner trip 2", owner));
        entityManager.persistAndFlush(new Trip("Other trip", other));

        List<Trip> ownerTrips = tripRepository.findByUserId(owner.getId());

        assertThat(ownerTrips)
                .hasSize(2)
                .extracting(Trip::getName)
                .containsExactlyInAnyOrder("Owner trip 1", "Owner trip 2");
    }

    @Test
    void findByUserId_ReturnsEmpty_WhenUserHasNoTrips() {
        User user = entityManager.persistAndFlush(new User("lonely@example.com", "hash", "Lonely", Role.USER));

        assertThat(tripRepository.findByUserId(user.getId())).isEmpty();
    }

    private Trip garminTrip(User user, Long garminActivityId) {
        Trip trip = new Trip("Garmin trip", user);
        trip.setGarminActivityId(garminActivityId);
        return trip;
    }

    @Test
    void existsByUserIdAndGarminActivityId_OnlyMatchesThatUsersActivity() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        User other = entityManager.persistAndFlush(new User("other@example.com", "hash", "Other", Role.USER));
        entityManager.persistAndFlush(garminTrip(owner, 100L));

        assertThat(tripRepository.existsByUserIdAndGarminActivityId(owner.getId(), 100L)).isTrue();
        assertThat(tripRepository.existsByUserIdAndGarminActivityId(owner.getId(), 200L)).isFalse();
        assertThat(tripRepository.existsByUserIdAndGarminActivityId(other.getId(), 100L)).isFalse();
    }

    @Test
    void uniqueConstraint_RejectsSameActivityTwiceForOneUser() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        tripRepository.saveAndFlush(garminTrip(owner, 100L));

        assertThatThrownBy(() -> tripRepository.saveAndFlush(garminTrip(owner, 100L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueConstraint_AllowsSameActivityForOtherUserAndManyAppTrips() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        User other = entityManager.persistAndFlush(new User("other@example.com", "hash", "Other", Role.USER));
        tripRepository.saveAndFlush(garminTrip(owner, 100L));
        tripRepository.saveAndFlush(garminTrip(other, 100L));
        tripRepository.saveAndFlush(garminTrip(owner, null));
        tripRepository.saveAndFlush(garminTrip(owner, null));

        assertThat(tripRepository.findByUserId(owner.getId())).hasSize(3);
    }
}
