package com.ole.turapp.repository;

import com.ole.turapp.model.Role;
import com.ole.turapp.model.Route;
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
class RouteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RouteRepository routeRepository;

    @Test
    void findByUserId_ReturnsOnlyThatUsersRoutes() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        User other = entityManager.persistAndFlush(new User("other@example.com", "hash", "Other", Role.USER));

        entityManager.persistAndFlush(new Route("Owner route", owner));
        entityManager.persistAndFlush(new Route("Other route", other));

        List<Route> ownerRoutes = routeRepository.findByUserId(owner.getId());

        assertThat(ownerRoutes)
                .hasSize(1)
                .extracting(Route::getName)
                .containsExactly("Owner route");
    }

    @Test
    void findByUserId_ReturnsEmpty_WhenUserHasNoRoutes() {
        User user = entityManager.persistAndFlush(new User("lonely@example.com", "hash", "Lonely", Role.USER));

        assertThat(routeRepository.findByUserId(user.getId())).isEmpty();
    }
}
