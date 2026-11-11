package com.renegade.telemetry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    Page<Telemetry> findByVehicle_Id(Long vehicleId, Pageable pageable);

    Page<Telemetry> findByVehicle_IdAndTimestampGreaterThanEqual(
            Long vehicleId, Instant from, Pageable pageable);

    Page<Telemetry> findByVehicle_IdAndTimestampLessThan(
            Long vehicleId, Instant to, Pageable pageable);

    Page<Telemetry> findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
            Long vehicleId,
            Instant from,
            Instant to,
            Pageable pageable);

    Optional<Telemetry> findTopByVehicle_IdOrderByTimestampDescIdDesc(Long vehicleId);

    Optional<Telemetry> findTopByVehicle_IdOrderByCreatedAtDescIdDesc(Long vehicleId);
}
