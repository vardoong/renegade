package com.renegade.vehicle.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleCreateRequest(
        @NotBlank
        String externalId,

        @NotBlank
        String modelName,

        @NotBlank
        String name
) {
}