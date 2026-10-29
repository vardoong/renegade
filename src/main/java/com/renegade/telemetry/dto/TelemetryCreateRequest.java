package com.renegade.telemetry.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TelemetryCreateRequest(
        @NotNull
        Long vehicleId,

        @NotNull
        Double speed,

        @NotNull
        Integer rpm,

        @NotNull
        Integer batteryLevel,

        Double temperature,

        Double latitude,

        Double longitude,

        @NotNull
        LocalDateTime timestamp
) {
}
