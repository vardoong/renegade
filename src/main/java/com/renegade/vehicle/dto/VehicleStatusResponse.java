package com.renegade.vehicle.dto;

import com.renegade.vehicle.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "차량 상태 응답")
public record VehicleStatusResponse(
        @Schema(
                description = "차량 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "차량 상태",
                example = "DRIVING"
        )
        VehicleStatus status,

        @Schema(
                description = "마지막 텔레메트리 수신 일시",
                example = "2026-10-30T15:30:00Z"
        )
        Instant lastReceivedAt
){}