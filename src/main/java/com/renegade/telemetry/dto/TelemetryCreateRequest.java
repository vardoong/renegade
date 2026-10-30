package com.renegade.telemetry.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record TelemetryCreateRequest(
        @NotNull
        @Positive(message = "차량 ID는 1 이상이어야 합니다.")
        Long vehicleId,

        @NotNull
        @DecimalMin(value = "0.0", message = "속도는 0 이상이어야 합니다.")
        Double speed,

        @NotNull
        @Min(value = 0, message = "RPM은 0 이상이어야 합니다.")
        Integer rpm,

        @NotNull
        @Min(value = 0, message = "배터리 잔량은 0 이상이어야 합니다.")
        @Max(value = 100, message = "배터리 잔량은 100 이하여야 합니다.")
        Integer batteryLevel,

        Double temperature,

        @Min(value = -90, message = "위도는 -90 이상이어야 합니다.")
        @Max(value = 90, message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @Min(value = -180, message = "경도는 -180 이상이어야 합니다.")
        @Max(value = 180, message = "경도는 180 이하여야 합니다.")
        Double longitude,

        @NotNull
        @PastOrPresent(message = "측정 시간은 미래일 수 없습니다.")
        LocalDateTime timestamp
) {
}
