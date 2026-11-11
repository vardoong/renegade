package com.renegade.telemetry;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
import com.renegade.vehicle.Vehicle;
import com.renegade.vehicle.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
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

    @Mock
    private TelemetryMetrics telemetryMetrics;

    @InjectMocks
    private TelemetryService telemetryService;

    private Vehicle vehicle;
    private Telemetry telemetry;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        // 테스트 전 트랜잭션 동기화 init
        TransactionSynchronizationManager.initSynchronization();
        timestamp = Instant.parse("2026-10-29T12:30:00Z");

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

    @AfterEach
    void tearDown() {
        // 테스트 후 트랜잭션 동기화 정리
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("텔레메트리 등록 성공")
    void createTelemetrySuccessfully() {
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

        verifyNoInteractions(telemetryMetrics);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(telemetryMetrics).recordSuccess();
        verify(telemetryMetrics, never()).recordFailure();
    }

    @Test
    @DisplayName("존재하지 않는 차량에 텔레메트리 등록 실패")
    void createTelemetryFailsWhenVehicleDoesNotExist() {
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

        verifyNoInteractions(telemetryMetrics);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(telemetryMetrics).recordFailure();
        verify(telemetryMetrics, never()).recordSuccess();
    }

    @Test
    @DisplayName("텔레메트리 단건 조회 성공")
    void getTelemetrySuccessfully() {
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
    @DisplayName("존재하지 않는 텔레메트리 조회 실패")
    void getTelemetryFailsWhenTelemetryDoesNotExist() {
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
    @DisplayName("차량별 텔레메트리 목록 조회 성공")
    void getTelemetryListByVehicleSuccessfully() {
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
                telemetryService.getList(1L, null, null, pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().speed())
                .isEqualTo(60.0);

        verify(telemetryRepository)
                .findByVehicle_Id(1L, pageable);
    }

    @Test
    @DisplayName("차량별 최신 텔레메트리 조회 성공")
    void getLatestTelemetryByVehicleSuccessfully() {
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
    @DisplayName("텔레메트리가 없는 차량의 최신 조회 실패")
    void getLatestTelemetryFailsWhenVehicleHasNoTelemetry() {
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
    @DisplayName("존재하지 않는 차량의 목록 조회 실패")
    void getTelemetryListFailsWhenVehicleDoesNotExist() {
        // given
        Long vehicleId = 999L;
        Pageable pageable = PageRequest.of(0, 20);

        when(vehicleRepository.existsById(vehicleId))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> telemetryService.getList(vehicleId, null, null, pageable))
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
    @DisplayName("존재하지 않는 차량의 최신 조회 실패")
    void getLatestTelemetryFailsWhenVehicleDoesNotExist() {
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
    @DisplayName("텔레메트리가 없는 차량의 목록 조회 성공")
    void getEmptyTelemetryListWhenVehicleHasNoTelemetry() {
        // given
        Long vehicleId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        when(vehicleRepository.existsById(vehicleId))
                .thenReturn(true);

        when(telemetryRepository.findByVehicle_Id(vehicleId, pageable))
                .thenReturn(Page.empty(pageable));

        // when
        Page<TelemetryResponse> response =
                telemetryService.getList(vehicleId, null, null, pageable);

        // then
        assertThat(response.isEmpty()).isTrue();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("from만 전달하면 해당 시각 이상의 이력을 조회한다")
    void getListWithFromOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(telemetryRepository.findByVehicle_IdAndTimestampGreaterThanEqual(1L, timestamp, pageable))
                .thenReturn(new PageImpl<>(List.of(telemetry), pageable, 1));

        Page<TelemetryResponse> result = telemetryService.getList(1L, timestamp, null, pageable);

        assertThat(result.getContent()).extracting(TelemetryResponse::timestamp).containsExactly(timestamp);
        verify(telemetryRepository).findByVehicle_IdAndTimestampGreaterThanEqual(1L, timestamp, pageable);
        verifyNoMoreInteractions(telemetryRepository);
    }

    @Test
    @DisplayName("to만 전달하면 해당 시각 미만의 이력을 조회한다")
    void getListWithToOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Instant to = timestamp.plusSeconds(1);
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(telemetryRepository.findByVehicle_IdAndTimestampLessThan(1L, to, pageable))
                .thenReturn(new PageImpl<>(List.of(telemetry), pageable, 1));

        Page<TelemetryResponse> result = telemetryService.getList(1L, null, to, pageable);

        assertThat(result.getContent()).extracting(TelemetryResponse::timestamp).containsExactly(timestamp);
        verify(telemetryRepository).findByVehicle_IdAndTimestampLessThan(1L, to, pageable);
        verifyNoMoreInteractions(telemetryRepository);
    }

    @Test
    @DisplayName("기간 양쪽을 전달하면 범위 조회 결과를 반환한다")
    void getListWithBothBounds() {
        Pageable pageable = PageRequest.of(0, 20);
        Instant to = timestamp.plusSeconds(1);
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(telemetryRepository.findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
                1L, timestamp, to, pageable))
                .thenReturn(new PageImpl<>(List.of(telemetry), pageable, 1));

        Page<TelemetryResponse> result = telemetryService.getList(1L, timestamp, to, pageable);

        assertThat(result.getContent()).extracting(TelemetryResponse::timestamp).containsExactly(timestamp);
        verify(telemetryRepository).findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
                1L, timestamp, to, pageable);
        verifyNoMoreInteractions(telemetryRepository);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("from이 to 이상이면 날짜 오류를 반환하고 이력은 조회하지 않는다")
    void getListRejectsInvalidRange(long seconds) {
        when(vehicleRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> telemetryService.getList(
                1L, timestamp, timestamp.plusSeconds(seconds), PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_REQUEST));
        verifyNoInteractions(telemetryRepository);
    }
}
