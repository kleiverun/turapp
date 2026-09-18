package com.ole.turapp.repository;

import com.ole.turapp.model.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
    List<RoutePoint> findByRouteIdOrderByPointOrderAsc(Long routeId);

    /** All points for all of a user's routes, in a single query (via route.user.id). */
    List<RoutePoint> findByRouteUserIdOrderByRouteIdAscPointOrderAsc(Long userId);

    int countByRouteId(Long routeId);

    void deleteByRouteId(Long routeId);
}
