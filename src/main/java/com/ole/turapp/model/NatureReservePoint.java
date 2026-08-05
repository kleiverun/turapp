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
@Table(name = "nature_reserve_points")
public class NatureReservePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserve_id", nullable = false)
    private NatureReserve reserve;

    @Column(name = "point_order", nullable = false)
    private int pointOrder;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    protected NatureReservePoint() {}

    public NatureReservePoint(NatureReserve reserve, int pointOrder, double latitude, double longitude) {
        this.reserve = reserve;
        this.pointOrder = pointOrder;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public NatureReserve getReserve() { return reserve; }
    public int getPointOrder() { return pointOrder; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
