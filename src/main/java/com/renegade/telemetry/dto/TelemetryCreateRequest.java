package com.renegade.telemetry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "텔레메트리 등록 요청")
public record TelemetryCreateRequest(
        @NotNull
        @Positive(message = "차량 ID는 1 이상이어야 합니다.")
        @Schema(
                description = "차량 ID",
                example = "1"
        )
        Long vehicleId,

        @NotNull
        @DecimalMin(value = "0.0", message = "속도는 0 이상이어야 합니다.")
        @Schema(
                description = "차량 속도 (km/h)",
                example = "78.2"
        )
        Double speed,

        @NotNull
        @Min(value = 0, message = "RPM은 0 이상이어야 합니다.")
        @Schema(
                description = "엔진 회전수 (RPM)",
                example = "2500"
        )
        Integer rpm,

        @NotNull
        @Min(value = 0, message = "배터리 잔량은 0 이상이어야 합니다.")
        @Max(value = 100, message = "배터리 잔량은 100 이하여야 합니다.")
        @Schema(
                description = "배터리 잔량 (%)",
                example = "87"
        )
        Integer batteryLevel,

        @Schema(
                description = "온도 (섭씨 °C)",
                example = "35.2",
                nullable = true
        )
        Double temperature,

        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        @Schema(
                description = "위도",
                example = "37.5665",
                nullable = true
        )
        Double latitude,

        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        @Schema(
                description = "경도",
                example = "126.978",
                nullable = true
        )
        Double longitude,

        @NotNull
        @PastOrPresent(message = "측정 시간은 미래일 수 없습니다.")
        @Schema(
                description = "텔레메트리 측정 시간",
                example = "2026-10-30T15:30:00"
        )
        LocalDateTime timestamp
) {
}
