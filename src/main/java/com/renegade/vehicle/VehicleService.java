package com.renegade.vehicle;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.telemetry.Telemetry;
import com.renegade.telemetry.TelemetryRepository;
import com.renegade.vehicle.dto.VehicleCreateRequest;
import com.renegade.vehicle.dto.VehicleResponse;
import com.renegade.vehicle.dto.VehicleStatusResponse;
import com.renegade.vehicle.enums.VehicleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TelemetryRepository telemetryRepository;
    private final VehicleStatusResolver vehicleStatusResolver;

    @Transactional
    public VehicleResponse create(VehicleCreateRequest req) {

        if(vehicleRepository.existsByExternalId(req.externalId())) {
            throw new BusinessException(ErrorCode.VEHICLE_ALREADY_EXISTS);
        }

        Vehicle vehicle = Vehicle.builder()
                .externalId(req.externalId())
                .modelName(req.modelName())
                .name(req.name())
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponse.from(savedVehicle);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(Long id){
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        return VehicleResponse.from(vehicle);
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> getList(Pageable pageable) {
        return vehicleRepository.findAll(pageable)
                .map(VehicleResponse::from);
    }

    @Transactional(readOnly = true)
    public VehicleStatusResponse getStatus(Long id){
        if (!vehicleRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        Optional<Telemetry> latest = telemetryRepository.findTopByVehicle_IdOrderByCreatedAtDescIdDesc(id);

        if(latest.isEmpty()) {
            return new VehicleStatusResponse(id, VehicleStatus.NO_DATA, null);
        }

        Telemetry telemetry = latest.get();

        return new VehicleStatusResponse(
                id,
                vehicleStatusResolver.resolve(telemetry, Instant.now()),
                telemetry.getCreatedAt()
        );
    }
}
