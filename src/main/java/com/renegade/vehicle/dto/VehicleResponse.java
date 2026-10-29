package com.renegade.vehicle.dto;

import com.renegade.vehicle.Vehicle;

import java.time.LocalDateTime;

public record VehicleResponse(
        Long id,
        String externalId,
        String modelName,
        String name,
        LocalDateTime createdAt
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getExternalId(),
                vehicle.getModelName(),
                vehicle.getName(),
                vehicle.getCreatedAt()
        );
    }
}
