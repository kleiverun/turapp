package com.ole.turapp.repository;

import com.ole.turapp.model.Role;
import com.ole.turapp.model.Route;
import com.ole.turapp.model.RoutePoint;
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
class RoutePointRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoutePointRepository routePointRepository;

    private RoutePoint point(Route route, int order, double lat, double lon) {
        RoutePoint point = new RoutePoint(order, lat, lon);
        point.setRoute(route);
        return point;
    }

    @Test
    void findByRouteIdOrderByPointOrderAsc_ReturnsPointsInOrder() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        Route route = entityManager.persistAndFlush(new Route("Trail A", owner));

        entityManager.persistAndFlush(point(route, 1, 59.91, 10.71));
        entityManager.persistAndFlush(point(route, 0, 59.9, 10.7));

        List<RoutePoint> points = routePointRepository.findByRouteIdOrderByPointOrderAsc(route.getId());

        assertThat(points)
                .extracting(RoutePoint::getPointOrder)
                .containsExactly(0, 1);
    }

    @Test
    void findByRouteUserIdOrderByRouteIdAscPointOrderAsc_ReturnsOnlyThatUsersPoints() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        User other = entityManager.persistAndFlush(new User("other@example.com", "hash", "Other", Role.USER));
        Route ownerRoute = entityManager.persistAndFlush(new Route("Owner route", owner));
        Route otherRoute = entityManager.persistAndFlush(new Route("Other route", other));

        entityManager.persistAndFlush(point(ownerRoute, 0, 59.9, 10.7));
        entityManager.persistAndFlush(point(otherRoute, 0, 10.0, 10.0));

        List<RoutePoint> points = routePointRepository
                .findByRouteUserIdOrderByRouteIdAscPointOrderAsc(owner.getId());

        assertThat(points).hasSize(1);
        assertThat(points.get(0).getRoute().getId()).isEqualTo(ownerRoute.getId());
    }

    @Test
    void countByRouteId_ReturnsNumberOfPoints() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        Route route = entityManager.persistAndFlush(new Route("Trail A", owner));
        entityManager.persistAndFlush(point(route, 0, 59.9, 10.7));
        entityManager.persistAndFlush(point(route, 1, 59.91, 10.71));

        assertThat(routePointRepository.countByRouteId(route.getId())).isEqualTo(2);
    }

    @Test
    void deleteByRouteId_RemovesAllPointsForRoute() {
        User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner", Role.USER));
        Route route = entityManager.persistAndFlush(new Route("Trail A", owner));
        entityManager.persistAndFlush(point(route, 0, 59.9, 10.7));

        routePointRepository.deleteByRouteId(route.getId());
        entityManager.flush();

        assertThat(routePointRepository.findByRouteIdOrderByPointOrderAsc(route.getId())).isEmpty();
    }
}
