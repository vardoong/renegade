package com.renegade.vehicle;

import com.renegade.global.config.SpringDataConfig;
import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.vehicle.dto.VehicleStatusResponse;
import com.renegade.vehicle.enums.VehicleStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.Instant;
import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@Import(SpringDataConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    @DisplayName("차량 단건 조회 성공")
    void getVehicleSuccessfully() throws Exception {
        // given
        VehicleResponse response = new VehicleResponse(
                1L,
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle",
                null
        );

        when(vehicleService.get(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.externalId").value("VEHICLE-001"))
                .andExpect(jsonPath("$.modelName").value("IONIQ 5"));

        verify(vehicleService).get(1L);
    }

    @Test
    @DisplayName("차량 목록 조회 성공")
    void getVehicleListSuccessfully() throws Exception {
        // given
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "id")
        );

        VehicleResponse response = new VehicleResponse(
                1L,
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle",
                null
        );

        when(vehicleService.getList(pageable))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        // when & then
        mockMvc.perform(get("/api/v1/vehicles")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].externalId").value("VEHICLE-001"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(vehicleService).getList(pageable);
    }

    @Test
    @DisplayName("차량 등록 성공")
    void createVehicleSuccessfully() throws Exception {
        // given
        VehicleCreateRequest request = new VehicleCreateRequest(
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle"
        );

        VehicleResponse response = new VehicleResponse(
                1L,
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle",
                null
        );

        when(vehicleService.create(any(VehicleCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.externalId").value("VEHICLE-001"))
                .andExpect(jsonPath("$.modelName").value("IONIQ 5"))
                .andExpect(jsonPath("$.name").value("Test Vehicle"));

        verify(vehicleService).create(any(VehicleCreateRequest.class));
    }

    @Test
    @DisplayName("차량 등록 시 필수값 누락으로 실패")
    void createVehicleFailsWhenRequiredFieldIsMissing() throws Exception {
        // given
        String invalidRequest = """
            {
                "externalId": "",
                "modelName": "IONIQ 5",
                "name": "Test Vehicle"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never())
                .create(any(VehicleCreateRequest.class));
    }

    @Test
    @DisplayName("존재하지 않는 차량 조회 시 404 반환")
    void getVehicleReturns404WhenNotFound() throws Exception {
        // given
        Long vehicleId = 999L;

        when(vehicleService.get(vehicleId))
                .thenThrow(new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isNotFound());

        verify(vehicleService).get(vehicleId);
    }

    @Test
    @DisplayName("중복 차량 등록 시 409 반환")
    void createVehicleReturns409WhenAlreadyExists() throws Exception {
        // given
        VehicleCreateRequest request = new VehicleCreateRequest(
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle"
        );

        when(vehicleService.create(any(VehicleCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.VEHICLE_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(vehicleService).create(any(VehicleCreateRequest.class));
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class, names = {"DRIVING", "PARKED", "OFFLINE"})
    @DisplayName("차량 상태와 마지막 서버 수신 시각을 JSON으로 반환한다")
    void getVehicleStatusSuccessfully(VehicleStatus status) throws Exception {
        Instant receivedAt = Instant.parse("2026-11-11T00:00:00Z");
        when(vehicleService.getStatus(1L))
                .thenReturn(new VehicleStatusResponse(1L, status, receivedAt));

        mockMvc.perform(get("/api/v1/vehicles/{id}/status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value(status.name()))
                .andExpect(jsonPath("$.lastReceivedAt").value(receivedAt.toString()));
        verify(vehicleService).getStatus(1L);
    }

    @Test
    @DisplayName("수집 이력이 없으면 200과 NO_DATA 및 null 수신 시각을 반환한다")
    void getVehicleStatusWithoutTelemetry() throws Exception {
        when(vehicleService.getStatus(1L))
                .thenReturn(new VehicleStatusResponse(1L, VehicleStatus.NO_DATA, null));

        mockMvc.perform(get("/api/v1/vehicles/{id}/status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("NO_DATA"))
                .andExpect(jsonPath("$.lastReceivedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("존재하지 않는 차량의 상태 조회는 404를 반환한다")
    void getVehicleStatusReturns404WhenNotFound() throws Exception {
        when(vehicleService.getStatus(999L))
                .thenThrow(new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/vehicles/{id}/status", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }
}