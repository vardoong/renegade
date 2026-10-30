package com.renegade.vehicle.dto;

import com.renegade.vehicle.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "차량 정보 응답")
public record VehicleResponse(
        @Schema(
                description = "차량 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "외부 시스템에서 관리하는 차량 식별자",
                example = "VEHICLE-001"
        )
        String externalId,

        @Schema(
                description = "모델명",
                example = "IONIQ 5"
        )
        String modelName,

        @Schema(
                description = "차량명",
                example = "My Test Vehicle"
        )
        String name,

        @Schema(
                description = "차량 등록 일시",
                example = "2026-10-30T15:30:00"
        )
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
