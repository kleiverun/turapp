package com.ole.turapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A single coordinate on a Route. Many RoutePoints in order form the trail line.
 * Coordinates are WGS84 latitude/longitude in decimal degrees (phone-GPS ready).
 */
@Entity
@Table(name = "route_point")
public class RoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * The route this point belongs to.
     * fetch = LAZY: the Route is only loaded from the DB when actually accessed,
     * so loading points does not automatically pull in the whole Route.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    /** Position of this point within the route, starting at 0. Keeps the line in order. */
    @Column(name = "point_order", nullable = false)
    private int pointOrder;

    /** Latitude in decimal degrees (WGS84), e.g. 59.881704. */
    @Column(name = "latitude", nullable = false)
    private double latitude;

    /** Longitude in decimal degrees (WGS84), e.g. 9.190247. */
    @Column(name = "longitude", nullable = false)
    private double longitude;

    /** JPA requires a no-argument constructor. */
    public RoutePoint() {
    }

    public RoutePoint(int pointOrder, double latitude, double longitude) {
        this.pointOrder = pointOrder;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public int getPointOrder() {
        return pointOrder;
    }

    public void setPointOrder(int pointOrder) {
        this.pointOrder = pointOrder;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}