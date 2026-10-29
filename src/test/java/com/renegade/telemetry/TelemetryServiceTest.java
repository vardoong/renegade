package com.renegade.telemetry;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
import com.renegade.vehicle.Vehicle;
import com.renegade.vehicle.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    private Vehicle vehicle;
    private Telemetry telemetry;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        timestamp = LocalDateTime.of(2026, 10, 29, 21, 30);

        vehicle = Vehicle.builder()
                .externalId("VEHICLE-001")
                .modelName("IONIQ 5")
                .name("Test Vehicle")
                .build();

        ReflectionTestUtils.setField(vehicle, "id", 1L);

        telemetry = Telemetry.builder()
                .vehicle(vehicle)
                .speed(60.0)
                .rpm(2000)
                .batteryLevel(80)
                .temperature(30.5)
                .latitude(37.5665)
                .longitude(126.9780)
                .timestamp(timestamp)
                .build();
    }

    @Test
    void 텔레메트리_등록_성공() {
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

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        when(telemetryRepository.save(any(Telemetry.class)))
                .thenReturn(telemetry);

        // when
        TelemetryResponse response = telemetryService.create(request);

        // then
        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.speed()).isEqualTo(60.0);
        assertThat(response.rpm()).isEqualTo(2000);
        assertThat(response.batteryLevel()).isEqualTo(80);

        ArgumentCaptor<Telemetry> captor =
                ArgumentCaptor.forClass(Telemetry.class);

        verify(telemetryRepository).save(captor.capture());

        assertThat(captor.getValue().getVehicle())
                .isSameAs(vehicle);
    }

    @Test
    void 존재하지_않는_차량에_텔레메트리_등록_실패() {
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

        when(vehicleRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> telemetryService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
                });

        verify(telemetryRepository, never())
                .save(any(Telemetry.class));
    }

    @Test
    void 텔레메트리_단건_조회_성공() {
        // given
        when(telemetryRepository.findById(1L))
                .thenReturn(Optional.of(telemetry));

        // when
        TelemetryResponse response = telemetryService.get(1L);

        // then
        assertThat(response.speed()).isEqualTo(60.0);
        assertThat(response.rpm()).isEqualTo(2000);
        assertThat(response.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void 존재하지_않는_텔레메트리_조회_실패() {
        // given
        when(telemetryRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> telemetryService.get(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.TELEMETRY_NOT_FOUND);
                });
    }

    @Test
    void 차량별_텔레메트리_목록_조회_성공() {
        // given
        Pageable pageable = PageRequest.of(0, 20);

        Page<Telemetry> page =
                new PageImpl<>(List.of(telemetry), pageable, 1);

        when(telemetryRepository.findByVehicle_Id(1L, pageable))
                .thenReturn(page);

        when(vehicleRepository.existsById(1L))
                .thenReturn(true);
        // when
        Page<TelemetryResponse> response =
                telemetryService.getList(1L, pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).speed())
                .isEqualTo(60.0);

        verify(telemetryRepository)
                .findByVehicle_Id(1L, pageable);
    }

    @Test
    void 차량별_최신_텔레메트리_조회_성공() {
        // given
        when(vehicleRepository.existsById(1L))
                .thenReturn(true);

        when(telemetryRepository
                .findTopByVehicle_IdOrderByTimestampDescIdDesc(1L))
                .thenReturn(Optional.of(telemetry));

        // when
        TelemetryResponse response =
                telemetryService.getLatest(1L);

        // then
        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.speed()).isEqualTo(60.0);
    }

    @Test
    void 텔레메트리가_없는_차량의_최신_조회_실패() {
        // given
        when(vehicleRepository.existsById(1L))
                .thenReturn(true);

        when(telemetryRepository
                .findTopByVehicle_IdOrderByTimestampDescIdDesc(1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> telemetryService.getLatest(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.TELEMETRY_NOT_FOUND);
                });
    }

    @Test
    void 존재하지_않는_차량의_목록_조회_실패() {
        // given
        Long vehicleId = 999L;
        Pageable pageable = PageRequest.of(0, 20);

        when(vehicleRepository.existsById(vehicleId))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> telemetryService.getList(vehicleId, pageable))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
                });

        verify(telemetryRepository, never())
                .findByVehicle_Id(any(), any());
    }

    @Test
    void 존재하지_않는_차량의_최신_조회_실패() {
        // given
        Long vehicleId = 999L;

        when(vehicleRepository.existsById(vehicleId))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> telemetryService.getLatest(vehicleId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
                });

        verify(telemetryRepository, never())
                .findTopByVehicle_IdOrderByTimestampDescIdDesc(any());
    }

    @Test
    void 텔레메트리가_없는_차량의_목록_조회_성공() {
        // given
        Long vehicleId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        when(vehicleRepository.existsById(vehicleId))
                .thenReturn(true);

        when(telemetryRepository.findByVehicle_Id(vehicleId, pageable))
                .thenReturn(Page.empty(pageable));

        // when
        Page<TelemetryResponse> response =
                telemetryService.getList(vehicleId, pageable);

        // then
        assertThat(response.isEmpty()).isTrue();
        assertThat(response.getTotalElements()).isZero();
    }
}