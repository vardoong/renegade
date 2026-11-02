package com.renegade.vehicle;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Builder
    private Vehicle(
            String externalId,
            String modelName,
            String name
    ) {
        this.externalId = externalId;
        this.modelName = modelName;
        this.name = name;
    }
}
