package com.renegade.vehicle;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder()
                .externalId("VEHICLE-001")
                .modelName("IONIQ 5")
                .name("Test Vehicle")
                .build();
    }

    @Test
    @DisplayName("차량 등록 성공")
    void createVehicleSuccessfully() {
        // given
        VehicleCreateRequest request = new VehicleCreateRequest(
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle"
        );

        when(vehicleRepository.existsByExternalId(request.externalId()))
                .thenReturn(false);

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenReturn(vehicle);

        // when
        VehicleResponse response = vehicleService.create(request);

        // then
        assertThat(response.externalId()).isEqualTo("VEHICLE-001");
        assertThat(response.modelName()).isEqualTo("IONIQ 5");

        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("차량 중복 등록 실패")
    void createVehicleFailsWhenAlreadyExists() {
        // given
        VehicleCreateRequest request = new VehicleCreateRequest(
                "VEHICLE-001",
                "IONIQ 5",
                "Test Vehicle"
        );

        when(vehicleRepository.existsByExternalId(request.externalId()))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> vehicleService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.VEHICLE_ALREADY_EXISTS);
                });

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("차량 단건 조회 성공")
    void getVehicleSuccessfully() {
        // given
        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        // when
        VehicleResponse response = vehicleService.get(1L);

        // then
        assertThat(response.externalId()).isEqualTo("VEHICLE-001");
        assertThat(response.name()).isEqualTo("Test Vehicle");
    }

    @Test
    @DisplayName("존재하지 않는 차량 조회 실패")
    void getVehicleFailsWhenNotFound() {
        // given
        when(vehicleRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> vehicleService.get(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("차량 목록 조회 성공")
    void getVehicleListSuccessfully() {
        // given
        Pageable pageable = PageRequest.of(0, 20);

        Page<Vehicle> vehicles =
                new PageImpl<>(List.of(vehicle), pageable, 1);

        when(vehicleRepository.findAll(pageable))
                .thenReturn(vehicles);

        // when
        Page<VehicleResponse> response =
                vehicleService.getList(pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);

        VehicleResponse result = response.getContent().getFirst();

        assertThat(result.externalId()).isEqualTo("VEHICLE-001");
        assertThat(result.modelName()).isEqualTo("IONIQ 5");

        verify(vehicleRepository).findAll(pageable);
    }
}