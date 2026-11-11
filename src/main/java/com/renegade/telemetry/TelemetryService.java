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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TelemetryService {
    private final TelemetryRepository telemetryRepository;
    private final VehicleRepository vehicleRepository;
    private final TelemetryMetrics telemetryMetrics;

    @Transactional
    public TelemetryResponse create(TelemetryCreateRequest req) {
        registerSaveMetrics();

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
            Long vehicleId,
            Instant from,
            Instant to,
            Pageable pageable) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        Page<Telemetry> result;
        if (from != null && to != null) {
            if(!from.isBefore(to)) {
                throw new BusinessException(ErrorCode.INVALID_DATE_REQUEST);
            }
            result = telemetryRepository
                    .findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
                            vehicleId, from, to, pageable);
        } else if (from != null) {
            result = telemetryRepository.findByVehicle_IdAndTimestampGreaterThanEqual(
                    vehicleId, from, pageable);
        } else if (to != null) {
            result = telemetryRepository.findByVehicle_IdAndTimestampLessThan(
                    vehicleId, to, pageable);
        } else {
            result = telemetryRepository.findByVehicle_Id(vehicleId, pageable);
        }

        return result.map(TelemetryResponse::from);
    }

    @Transactional(readOnly = true)
    public TelemetryResponse getLatest(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }

        Telemetry telemetry = telemetryRepository.findTopByVehicle_IdOrderByTimestampDescIdDesc(vehicleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TELEMETRY_NOT_FOUND));

        return TelemetryResponse.from(telemetry);
    }

    private void registerSaveMetrics() {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            telemetryMetrics.recordSuccess();
                        } else if (status == STATUS_ROLLED_BACK) {
                            telemetryMetrics.recordFailure();
                        }
                    }
                }
        );
    }
}
