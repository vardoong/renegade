package com.renegade.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "차량 등록 요청")
public record VehicleCreateRequest(
        @Schema(
                description = "외부 시스템에서 관리하는 차량 식별자 (중복 등록 불가)",
                example = "VEHICLE-001"
        )
        @NotBlank
        String externalId,

        @Schema(
                description = "모델명",
                example = "IONIQ 5"
        )
        @NotBlank
        String modelName,

        @Schema(
                description = "차량명",
                example = "My Test Vehicle"
        )
        @NotBlank
        String name
) {
}