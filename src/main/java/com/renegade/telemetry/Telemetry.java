package com.renegade.telemetry;

import com.renegade.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "telemetry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private Double speed;

    @Column(nullable = false)
    private Integer rpm;

    @Column(nullable = false)
    private Integer batteryLevel;

    @Column
    private Double temperature;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    private Telemetry(
            Vehicle vehicle,
            Double speed,
            Integer rpm,
            Integer batteryLevel,
            Double temperature,
            Double latitude,
            Double longitude,
            LocalDateTime timestamp
    ) {
        this.vehicle = vehicle;
        this.speed = speed;
        this.rpm = rpm;
        this.batteryLevel = batteryLevel;
        this.temperature = temperature;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}
