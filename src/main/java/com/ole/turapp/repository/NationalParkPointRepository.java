package com.ole.turapp.repository;

import com.ole.turapp.model.NationalParkPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NationalParkPointRepository extends JpaRepository<NationalParkPoint, Long> {
}
