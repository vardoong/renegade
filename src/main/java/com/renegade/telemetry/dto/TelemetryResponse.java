package com.renegade.telemetry.dto;

import com.renegade.telemetry.Telemetry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "텔레메트리 응답")
public record TelemetryResponse(
        @Schema(
                description = "텔레메트리 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "차량 ID",
                example = "1"
        )
        Long vehicleId,

        @Schema(
                description = "차량 속도 (km/h)",
                example = "78.2"
        )
        Double speed,

        @Schema(
                description = "엔진 회전수 (RPM)",
                example = "2500"
        )
        Integer rpm,

        @Schema(
                description = "배터리 잔량 (%)",
                example = "87"
        )
        Integer batteryLevel,

        @Schema(
                description = "온도 (섭씨 °C)",
                example = "35.2"
        )
        Double temperature,

        @Schema(
                description = "위도",
                example = "37.5665"
        )
        Double latitude,

        @Schema(
                description = "경도",
                example = "126.978"
        )
        Double longitude,

        @Schema(
                description = "차량에서 센서 데이터를 측정한 시간",
                example = "2026-10-30T15:30:00"
        )
        LocalDateTime timestamp,

        @Schema(
                description = "서버에 텔레메트리를 저장한 시간",
                example = "2026-10-30T15:30:00"
        )
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
