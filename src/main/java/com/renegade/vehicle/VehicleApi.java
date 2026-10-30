package com.renegade.vehicle;

import com.renegade.global.exception.ErrorResponse;
import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
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

@Tag(name = "차량", description = "차량 등록 및 조회 API")
public interface VehicleApi {

    @Operation(summary = "차량 정보 등록")
    @ApiResponse(
            responseCode = "201",
            description = "차량 등록 성공",
            content = @Content(
                    schema = @Schema(implementation = VehicleResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "요청 데이터 검증 실패",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    ResponseEntity<VehicleResponse> create(
            VehicleCreateRequest vehicleCreateRequest
    );

    @Operation(summary = "차량 정보 조회")
    @ApiResponse(
            responseCode = "200",
            description = "차량 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = VehicleResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "차량을 찾을 수 없음",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    ResponseEntity<VehicleResponse> findById(
            @Parameter(
                    description = "조회할 차량 ID",
                    example = "1"
            ) Long id
    );

    @Operation(
            summary = "차량 목록 조회",
            description = "차량 목록을 페이지 단위로 조회합니다. " +
                    "기본 페이지 크기는 20건이며, 차량 ID를 기준으로 내림차순 정렬합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "차량 목록 조회 성공"
    )
    ResponseEntity<Page<VehicleResponse>> getList(
            @ParameterObject Pageable pageable
    );
}
