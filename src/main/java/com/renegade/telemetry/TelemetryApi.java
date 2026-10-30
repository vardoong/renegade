package com.renegade.telemetry;

import com.renegade.global.exception.ErrorResponse;
import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "텔레메트리",
        description = "차량 센서 데이터 등록 및 조회 API"
)
public interface TelemetryApi {

    @Operation(summary = "텔레메트리 등록")
    @ApiResponse(
            responseCode = "201",
            description = "텔레메트리 등록 성공",
            content = @Content(
                    schema = @Schema(implementation = TelemetryResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "요청 데이터 검증 실패",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "등록할 차량을 찾을 수 없음",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    ResponseEntity<TelemetryResponse> create(
            TelemetryCreateRequest request
    );

    @Operation(summary = "텔레메트리 단건 조회")
    @ApiResponse(
            responseCode = "200",
            description = "텔레메트리 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = TelemetryResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "텔레메트리 찾을 수 없음",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    ResponseEntity<TelemetryResponse> findById(
            @Parameter(
                    description = "조회할 텔레메트리 ID",
                    example = "1"
            ) Long id
    );

    @Operation(
            summary = "텔레메트리 목록 조회",
            description = "차량 ID를 기준으로 텔레메트리 이력을 페이지 단위로 조회합니다. " +
                    "기본 페이지 크기는 20건이며, timestamp와 ID를 기준으로 내림차순 정렬합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "텔레메트리 목록 조회 성공"
    )
    ResponseEntity<Page<TelemetryResponse>> findList(
            @Parameter(
                    description = "조회할 차량 ID",
                    example = "1"
            ) Long vehicleId,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "차량의 최신 텔레메트리 조회")
    @ApiResponse(
            responseCode = "200",
            description = "텔레메트리 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = TelemetryResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "차량이 존재하지 않거나 수집된 텔레메트리가 없음",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    ResponseEntity<TelemetryResponse> findLatest(
            @Parameter(
                    description = "조회할 차량 ID",
                    example = "1"
            ) Long vehicleId
    );
}