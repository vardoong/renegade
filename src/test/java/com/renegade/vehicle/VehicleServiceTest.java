package com.renegade.vehicle;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import org.junit.jupiter.api.BeforeEach;
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
    void 차량_등록_성공() {
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
    void 차량_중복_등록_실패() {
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
    void 차량_단건_조회_성공() {
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
    void 존재하지_않는_차량_조회_실패() {
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
    void 차량_목록_조회_성공() {
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

        VehicleResponse result = response.getContent().get(0);

        assertThat(result.externalId()).isEqualTo("VEHICLE-001");
        assertThat(result.modelName()).isEqualTo("IONIQ 5");

        verify(vehicleRepository).findAll(pageable);
    }
}