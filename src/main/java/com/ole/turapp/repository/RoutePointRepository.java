package com.ole.turapp.repository;

import com.ole.turapp.model.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
    List<RoutePoint> findByRouteIdOrderByPointOrderAsc(Long routeId);

    /** Alle punkter for alle rutene til en bruker, i én spørring (via route.user.id). */
    List<RoutePoint> findByRouteUserIdOrderByRouteIdAscPointOrderAsc(Long userId);

    int countByRouteId(Long routeId);

    void deleteByRouteId(Long routeId);
}
