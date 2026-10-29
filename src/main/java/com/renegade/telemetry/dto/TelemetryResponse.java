package com.renegade.telemetry.dto;

import com.renegade.telemetry.Telemetry;

import java.time.LocalDateTime;

public record TelemetryResponse(
        Long id,
        Long vehicleId,
        Double speed,
        Integer rpm,
        Integer batteryLevel,
        Double temperature,
        Double latitude,
        Double longitude,
        LocalDateTime timestamp,
        LocalDateTime createdAt
) {
    public static TelemetryResponse from(Telemetry telemetry) {
        return new TelemetryResponse(
                telemetry.getId(),
                telemetry.getVehicle().getId(),
                telemetry.getSpeed(),
                telemetry.getRpm(),
                telemetry.getBatteryLevel(),
                telemetry.getTemperature(),
                telemetry.getLatitude(),
                telemetry.getLongitude(),
                telemetry.getTimestamp(),
                telemetry.getCreatedAt()
        );
    }
}
