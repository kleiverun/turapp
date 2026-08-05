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

@Entity
@Table(name = "national_park_points")
public class NationalParkPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "park_id", nullable = false)
    private NationalPark park;

    @Column(name = "point_order", nullable = false)
    private int pointOrder;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    protected NationalParkPoint() {}

    public NationalParkPoint(NationalPark park, int pointOrder, double latitude, double longitude) {
        this.park = park;
        this.pointOrder = pointOrder;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public NationalPark getPark() { return park; }
    public int getPointOrder() { return pointOrder; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
