package com.renegade.telemetry;

import com.renegade.vehicle.Vehicle;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "telemetry")
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
}
