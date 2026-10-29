package com.renegade.telemetry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    Page<Telemetry> findByVehicle_Id(Long vehicleId, Pageable pageable);
    Optional<Telemetry> findTopByVehicle_IdOrderByTimestampDesc(Long vehicleId);
}
