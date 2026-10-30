package com.renegade.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 오류 응답")
public record ErrorResponse(
        @Schema(description = "HTTP 상태 코드", example = "404")
        int status,

        @Schema(description = "애플리케이션 오류 코드", example = "VEHICLE_NOT_FOUND")
        String code,

        @Schema(description = "오류에 대한 설명", example = "차량을 찾을 수 없습니다.")
        String message
) {
    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.name(),
                errorCode.getMessage()
        );
    }
}
