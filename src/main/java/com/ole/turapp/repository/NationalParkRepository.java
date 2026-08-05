package com.ole.turapp.repository;

import com.ole.turapp.model.NationalPark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NationalParkRepository extends JpaRepository<NationalPark, Long> {
}
