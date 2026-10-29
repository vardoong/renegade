package com.renegade.telemetry;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TelemetryService telemetryService;

    private final LocalDateTime timestamp =
            LocalDateTime.of(2026, 10, 29, 21, 30);

    private TelemetryResponse response() {
        return new TelemetryResponse(
                10L,
                1L,
                60.0,
                2000,
                80,
                30.5,
                37.5665,
                126.9780,
                timestamp,
                null
        );
    }

    // 1. Telemetry 등록 성공
    @Test
    void 텔레메트리_등록_성공() throws Exception {
        // given
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                1L,
                60.0,
                2000,
                80,
                30.5,
                37.5665,
                126.9780,
                timestamp
        );

        when(telemetryService.create(any(TelemetryCreateRequest.class)))
                .thenReturn(response());

        // when & then
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.vehicleId").value(1))
                .andExpect(jsonPath("$.speed").value(60.0))
                .andExpect(jsonPath("$.rpm").value(2000))
                .andExpect(jsonPath("$.batteryLevel").value(80));

        verify(telemetryService)
                .create(any(TelemetryCreateRequest.class));
    }

    // 2. Validation 실패
    @Test
    void 텔레메트리_등록_필수값_누락_실패() throws Exception {
        // given
        String invalidRequest = """
                {
                    "vehicleId": null,
                    "speed": 60.0,
                    "rpm": 2000,
                    "batteryLevel": 80,
                    "temperature": 30.5,
                    "latitude": 37.5665,
                    "longitude": 126.9780,
                    "timestamp": "2026-10-29T21:30:00"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(telemetryService, never())
                .create(any(TelemetryCreateRequest.class));
    }

    // 3. 존재하지 않는 차량에 Telemetry 등록
    @Test
    void 존재하지_않는_차량에_텔레메트리_등록_실패() throws Exception {
        // given
        TelemetryCreateRequest request = new TelemetryCreateRequest(
                999L,
                60.0,
                2000,
                80,
                30.5,
                37.5665,
                126.9780,
                timestamp
        );

        when(telemetryService.create(any(TelemetryCreateRequest.class)))
                .thenThrow(
                        new BusinessException(ErrorCode.VEHICLE_NOT_FOUND)
                );

        // when & then
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // 4. Telemetry 단건 조회 성공
    @Test
    void 텔레메트리_단건_조회_성공() throws Exception {
        // given
        when(telemetryService.get(10L))
                .thenReturn(response());

        // when & then
        mockMvc.perform(get("/api/v1/telemetry/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.vehicleId").value(1))
                .andExpect(jsonPath("$.speed").value(60.0))
                .andExpect(jsonPath("$.rpm").value(2000));

        verify(telemetryService).get(10L);
    }

    // 5. 존재하지 않는 Telemetry 단건 조회
    @Test
    void 존재하지_않는_텔레메트리_조회_실패() throws Exception {
        // given
        when(telemetryService.get(999L))
                .thenThrow(
                        new BusinessException(ErrorCode.TELEMETRY_NOT_FOUND)
                );

        // when & then
        mockMvc.perform(get("/api/v1/telemetry/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(telemetryService).get(999L);
    }

    // 6. 차량별 Telemetry 목록 조회 성공
    @Test
    void 차량별_텔레메트리_목록_조회_성공() throws Exception {
        // given
        Long vehicleId = 1L;

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.desc("timestamp"),
                        Sort.Order.desc("id")
                )
        );

        Page<TelemetryResponse> page = new PageImpl<>(
                List.of(response()),
                pageable,
                1
        );

        when(telemetryService.getList(vehicleId, pageable))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get(
                        "/api/v1/vehicles/{vehicleId}/telemetry",
                        vehicleId
                )
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].vehicleId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(telemetryService)
                .getList(vehicleId, pageable);
    }

    // 7. Telemetry가 없는 차량의 목록 조회
    @Test
    void 텔레메트리가_없는_차량의_목록_조회_성공() throws Exception {
        // given
        Long vehicleId = 1L;

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.desc("timestamp"),
                        Sort.Order.desc("id")
                )
        );

        when(telemetryService.getList(vehicleId, pageable))
                .thenReturn(Page.empty(pageable));

        // when & then
        mockMvc.perform(get(
                        "/api/v1/vehicles/{vehicleId}/telemetry",
                        vehicleId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(telemetryService)
                .getList(vehicleId, pageable);
    }

    // 8. 존재하지 않는 차량의 Telemetry 목록 조회
    @Test
    void 존재하지_않는_차량의_목록_조회_실패() throws Exception {
        // given
        Long vehicleId = 999L;

        when(telemetryService.getList(
                eq(vehicleId),
                any(Pageable.class)
        )).thenThrow(
                new BusinessException(ErrorCode.VEHICLE_NOT_FOUND)
        );

        // when & then
        mockMvc.perform(get(
                        "/api/v1/vehicles/{vehicleId}/telemetry",
                        vehicleId
                ))
                .andExpect(status().isNotFound());
    }

    // 9. 최신 Telemetry 조회 성공
    @Test
    void 차량별_최신_텔레메트리_조회_성공() throws Exception {
        // given
        Long vehicleId = 1L;

        when(telemetryService.getLatest(vehicleId))
                .thenReturn(response());

        // when & then
        mockMvc.perform(get(
                        "/api/v1/vehicles/{vehicleId}/telemetry/latest",
                        vehicleId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.vehicleId").value(1))
                .andExpect(jsonPath("$.speed").value(60.0));

        verify(telemetryService)
                .getLatest(vehicleId);
    }

    // 10. Telemetry가 없는 차량의 최신 조회
    @Test
    void 텔레메트리가_없는_차량의_최신_조회_실패() throws Exception {
        // given
        Long vehicleId = 1L;

        when(telemetryService.getLatest(vehicleId))
                .thenThrow(
                        new BusinessException(ErrorCode.TELEMETRY_NOT_FOUND)
                );

        // when & then
        mockMvc.perform(get(
                        "/api/v1/vehicles/{vehicleId}/telemetry/latest",
                        vehicleId
                ))
                .andExpect(status().isNotFound());
    }

    // 11. 존재하지 않는 차량의 최신 조회
    @Test
    void 존재하지_않는_차량의_최신_조회_실패() throws Exception {
        // given
        Long vehicleId = 999L;

        when(telemetryService.getLatest(vehicleId))
                .thenThrow(
                        new BusinessException(ErrorCode.VEHICLE_NOT_FOUND)
                );

        // when & then
        mockMvc.perform(get(
                        "/api/v1/vehicles/{vehicleId}/telemetry/latest",
                        vehicleId
                ))
                .andExpect(status().isNotFound());
    }
}