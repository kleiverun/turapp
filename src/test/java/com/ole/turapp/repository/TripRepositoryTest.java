package com.ole.turapp.repository;

import com.ole.turapp.model.Role;
import com.ole.turapp.model.Trip;
import com.ole.turapp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
