package com.renegade.vehicle;

import com.renegade.telemetry.Telemetry;
import com.renegade.vehicle.enums.VehicleStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class VehicleStatusResolver {

    private final Duration offlineThreshold;

    public VehicleStatusResolver(
            @Value("${renegade.vehicle.offline-threshold:3m}")
            Duration offlineThreshold
    ) {
        this.offlineThreshold = offlineThreshold;
    }

    public VehicleStatus resolve(Telemetry telemetry, Instant now) {
        Instant offlineAt = telemetry.getCreatedAt()
                .plus(offlineThreshold);

        if (!now.isBefore(offlineAt)) {
            return VehicleStatus.OFFLINE;
        }

        return telemetry.getSpeed() > 0
                ? VehicleStatus.DRIVING
                : VehicleStatus.PARKED;
    }
}