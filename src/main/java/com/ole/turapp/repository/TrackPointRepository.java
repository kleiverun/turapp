package com.ole.turapp.repository;

import com.ole.turapp.model.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {
    List<TrackPoint> findByTripIdOrderByTimestampAsc(Long tripId);

    void deleteByTripId(Long tripId);
}