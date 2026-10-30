package com.renegade.telemetry;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Test
    @DisplayName("텔레메트리 등록 성공")
    void createTelemetrySuccessfully() throws Exception {
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

    @Test
    @DisplayName("필수값 누락 시 텔레메트리 등록 실패 - 400")
    void createTelemetryWithMissingRequiredField() throws Exception {
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

    @Test
    @DisplayName("존재하지 않는 차량에 텔레메트리 등록 실패 - 404")
    void createTelemetryWithNonExistentVehicle() throws Exception {
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

    @Test
    @DisplayName("텔레메트리 단건 조회 성공")
    void getTelemetrySuccessfully() throws Exception {
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

    @Test
    @DisplayName("존재하지 않는 텔레메트리 조회 실패 - 404")
    void getNonExistentTelemetry() throws Exception {
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

    @Test
    @DisplayName("차량별 텔레메트리 목록 조회 성공")
    void getTelemetryListByVehicleSuccessfully() throws Exception {
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

    @Test
    @DisplayName("텔레메트리가 없는 차량의 목록 조회 성공 - 빈 페이지 반환")
    void getEmptyTelemetryListByVehicle() throws Exception {
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

    @Test
    @DisplayName("존재하지 않는 차량의 텔레메트리 목록 조회 실패 - 404")
    void getTelemetryListByNonExistentVehicle() throws Exception {
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

    @Test
    @DisplayName("차량별 최신 텔레메트리 조회 성공")
    void getLatestTelemetryByVehicleSuccessfully() throws Exception {
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

    @Test
    @DisplayName("텔레메트리가 없는 차량의 최신 텔레메트리 조회 실패 - 404")
    void getLatestTelemetryFromVehicleWithNoTelemetry() throws Exception {
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

    @Test
    @DisplayName("존재하지 않는 차량의 최신 텔레메트리 조회 실패 - 404")
    void getLatestTelemetryByNonExistentVehicle() throws Exception {
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

    private String telemetryJsonWith(String field, String jsonValue) {
        Map<String, String> values = new LinkedHashMap<>();

        values.put("vehicleId", "1");
        values.put("speed", "60.0");
        values.put("rpm", "2000");
        values.put("batteryLevel", "80");
        values.put("temperature", "30.5");
        values.put("latitude", "37.5665");
        values.put("longitude", "126.9780");
        values.put("timestamp", "\"2026-10-29T21:30:00\"");

        if (!values.containsKey(field)) {
            throw new IllegalArgumentException("Unknown telemetry field: " + field);
        }

        values.put(field, jsonValue);

        return values.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    private void assertInvalidTelemetryField(
            String field,
            String jsonValue
    ) throws Exception {

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJsonWith(field, jsonValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(telemetryService, never())
                .create(any(TelemetryCreateRequest.class));
    }

    @ParameterizedTest
    @DisplayName("속도 경계값: 음수는 400을 반환한다")
    @ValueSource(doubles = {-0.1, -1.0})
    void createTelemetryRejectsNegativeSpeed(double speed) throws Exception {
        assertInvalidTelemetryField("speed", Double.toString(speed));
    }

    @ParameterizedTest
    @DisplayName("RPM 경계값: 음수는 400을 반환한다")
    @ValueSource(ints = {-1, -10})
    void createTelemetryRejectsNegativeRpm(int rpm) throws Exception {
        assertInvalidTelemetryField("rpm", Integer.toString(rpm));
    }

    @ParameterizedTest
    @DisplayName("배터리 잔량 경계값: 0에서 100 사이값이 아니면 400을 반환한다")
    @ValueSource(ints = {-1, 101})
    void createTelemetryRejectsOurOfRangeBatteryLevel(int batteryLevel) throws Exception {
        assertInvalidTelemetryField("batteryLevel", Integer.toString(batteryLevel));
    }

    @ParameterizedTest
    @DisplayName("위도 경계값: -90에서 90 사이값이 아니면 400을 반환한다")
    @ValueSource(doubles = {-90.9, 90.001})
    void createTelemetryRejectsOurOfRangeLatitude(double latitude) throws Exception {
        assertInvalidTelemetryField("latitude", Double.toString(latitude));
    }

    @ParameterizedTest
    @DisplayName("경도 경계값: -180에서 180 사이값이 아니면 400을 반환한다")
    @ValueSource(doubles = {-180.0002, 181.2})
    void createTelemetryRejectsOurOfRangeLongitude(double longitude) throws Exception {
        assertInvalidTelemetryField("longitude", Double.toString(longitude));
    }

    @Test
    @DisplayName("타임스탬프 경계값: 미래 시각이면 400을 반환한다")
    void createTelemetryRejectsFutureTimestamp() throws Exception {
        LocalDateTime futureTimestamp = LocalDateTime.now().plusDays(1);

        assertInvalidTelemetryField(
                "timestamp",
                "\"" + futureTimestamp + "\""
        );
    }

    @ParameterizedTest
    @DisplayName("필수 필드가 null이면 400을 반환한다")
    @ValueSource(strings = {"vehicleId", "speed", "rpm", "batteryLevel", "timestamp"})
    void createTelemetryRejectsNullRequiredFields(String field) throws Exception {
        assertInvalidTelemetryField(field, "null");
    }

    @ParameterizedTest
    @DisplayName("선택 필드가 null이어도 텔레메트리 등록에 성공한다")
    @ValueSource(strings = {"temperature", "latitude", "longitude"})
    void createTelemetryAcceptsNullOptionalFields(String field) throws Exception {
        when(telemetryService.create(any(TelemetryCreateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJsonWith(field, "null")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));

        verify(telemetryService)
                .create(any(TelemetryCreateRequest.class));
    }

    @Test
    @DisplayName("잘못된 JSON 문법으로 등록 요청 시 400과 INVALID_REQUEST를 반환한다 ( } 누락 )")
    void createTelemetryRejectsMalformedJson() throws Exception {
        // given
        String malformedJson = """
            {
                "vehicleId": 1,
                "speed": 60.0,
                "rpm": 2000,
            """;

        // when & then
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(telemetryService, never())
                .create(any(TelemetryCreateRequest.class));
    }

    @ParameterizedTest
    @DisplayName("필드 데이터 타입이 잘못되면 400과 INVALID_REQUEST를 반환한다")
    @ValueSource(strings = {
            "vehicleId",
            "speed",
            "rpm",
            "batteryLevel",
            "timestamp"
    })
    void createTelemetryRejectsInvalidFieldTypes(String field) throws Exception {
        // given
        String invalidValue = switch (field) {
            case "timestamp" -> "12345";
            default -> "\"not-a-number\"";
        };

        // when & then
        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJsonWith(field, invalidValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(telemetryService, never())
                .create(any(TelemetryCreateRequest.class));
    }

    private void assertValidTelemetryField(
            String field,
            String jsonValue
    ) throws Exception {

        when(telemetryService.create(any(TelemetryCreateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJsonWith(field, jsonValue)))
                .andExpect(status().isCreated());

        verify(telemetryService)
                .create(any(TelemetryCreateRequest.class));
    }

    @ParameterizedTest
    @DisplayName("속도가 정상 경계값이면 201을 반환한다")
    @ValueSource(doubles = {0.0, 0.1})
    void createTelemetryAcceptsValidSpeed(double speed) throws Exception {
        assertValidTelemetryField("speed", Double.toString(speed));
    }

    @ParameterizedTest
    @DisplayName("RPM이 정상 경계값이면 201을 반환한다")
    @ValueSource(ints = {0, 1})
    void createTelemetryAcceptsValidRpm(int rpm) throws Exception {
        assertValidTelemetryField("rpm", Integer.toString(rpm));
    }

    @ParameterizedTest
    @DisplayName("배터리 잔량이 정상 경계값이면 201을 반환한다")
    @ValueSource(ints = {0, 99, 100})
    void createTelemetryAcceptsValidBatteryLevel(int batteryLevel) throws Exception {
        assertValidTelemetryField(
                "batteryLevel",
                Integer.toString(batteryLevel)
        );
    }

    @ParameterizedTest
    @DisplayName("위도가 정상 경계값이면 201을 반환한다")
    @ValueSource(doubles = {-90.0, 90.0})
    void createTelemetryAcceptsValidLatitude(double latitude) throws Exception {
        assertValidTelemetryField("latitude", Double.toString(latitude));
    }

    @ParameterizedTest
    @DisplayName("경도가 정상 경계값이면 201을 반환한다")
    @ValueSource(doubles = {-180.0, 180.0})
    void createTelemetryAcceptsValidLongitude(double longitude) throws Exception {
        assertValidTelemetryField("longitude", Double.toString(longitude));
    }
}