package com.ole.turapp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "national_parks")
public class NationalPark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "external_id")
    private String externalId;

    @OneToMany(mappedBy = "park", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pointOrder ASC")
    private List<NationalParkPoint> points = new ArrayList<>();

    protected NationalPark() {}

    public NationalPark(String name, String externalId) {
        this.name = name;
        this.externalId = externalId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getExternalId() { return externalId; }
    public List<NationalParkPoint> getPoints() { return points; }
}
