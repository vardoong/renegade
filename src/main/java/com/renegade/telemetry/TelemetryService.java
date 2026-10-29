package com.renegade.telemetry;

import com.renegade.global.exception.BusinessException;
import com.renegade.global.exception.ErrorCode;
import com.renegade.telemetry.dto.TelemetryCreateRequest;
import com.renegade.telemetry.dto.TelemetryResponse;
import com.renegade.vehicle.Vehicle;
import com.renegade.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelemetryService {
    private final TelemetryRepository telemetryRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public TelemetryResponse create(TelemetryCreateRequest req) {
        Vehicle vehicle = vehicleRepository.findById(req.vehicleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        Telemetry telemetry = Telemetry.builder()
                .vehicle(vehicle)
                .speed(req.speed())
                .rpm(req.rpm())
                .batteryLevel(req.batteryLevel())
                .temperature(req.temperature())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .timestamp(req.timestamp())
                .build();

        Telemetry savedTelemetry = telemetryRepository.save(telemetry);

        return TelemetryResponse.from(savedTelemetry);
    }

    @Transactional(readOnly = true)
    public TelemetryResponse get(Long id) {
        Telemetry telemetry = telemetryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TELEMETRY_NOT_FOUND));

        return TelemetryResponse.from(telemetry);
    }

    @Transactional(readOnly = true)
    public Page<TelemetryResponse> getList(
            Pageable pageable,
            Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        return telemetryRepository.findByVehicle_Id(vehicle.getId(), pageable)
                .map(TelemetryResponse::from);
    }

    @Transactional(readOnly = true)
    public TelemetryResponse getLatest(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND));

        Telemetry telemetry = telemetryRepository.findTopByVehicle_IdOrderByTimestampDescIdDesc(vehicle.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TELEMETRY_NOT_FOUND));

        return TelemetryResponse.from(telemetry);
    }
}
