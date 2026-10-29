package com.renegade.global.exception;

public record ErrorResponse(
        int status,
        String code,
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
