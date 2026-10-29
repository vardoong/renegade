package com.renegade.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Vehicle
    VEHICLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 차량입니다."),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 차량입니다.");

    private final HttpStatus status;
    private final String message;
}
