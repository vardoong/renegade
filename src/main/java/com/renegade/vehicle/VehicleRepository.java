package com.renegade.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
