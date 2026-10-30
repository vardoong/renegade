package com.renegade.telemetry;

import com.renegade.global.TestcontainersConfiguration;
import com.renegade.vehicle.Vehicle;
import com.renegade.vehicle.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class TelemetryRepositoryIntegrationTest {

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Vehicle saveVehicle(String externalId) {
        return vehicleRepository.saveAndFlush(Vehicle.builder()
                .externalId(externalId)
                .modelName("IONIQ 5")
                .name("Test Vehicle")
                .build());
    }

    private Telemetry saveTelemetry(Vehicle vehicle, LocalDateTime timestamp) {
        return telemetryRepository.saveAndFlush(Telemetry.builder()
                .vehicle(vehicle)
                .speed(60.0)
                .rpm(2000)
                .batteryLevel(80)
                .temperature(30.5)
                .latitude(37.5665)
                .longitude(126.9780)
                .timestamp(timestamp)
                .build());
    }

    @Test
    @DisplayName("텔레메트리를 저장하고 조회할 수 있다")
    void shouldSaveAndFindTelemetry()  {
        // given
        Vehicle vehicle = saveVehicle("VEHICLE-001");
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 30, 10, 0);

        // when
        Telemetry saved = saveTelemetry(vehicle, timestamp);
        Telemetry found = telemetryRepository.findById(saved.getId())
                .orElseThrow();

        // then
        assertThat(found.getId()).isNotNull();
        assertThat(found.getVehicle().getId()).isEqualTo(vehicle.getId());
        assertThat(found.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("차량별 텔레메트리를 시간과 ID 내림차순으로 페이징 조회한다")
    void shouldFindTelemetryByVehiclePagedAndSortedDescending() {
        // given
        Vehicle vehicle = saveVehicle("VEHICLE-001");
        Vehicle other = saveVehicle("VEHICLE-002");

        LocalDateTime base = LocalDateTime.of(2026, 10, 30, 10, 0);

        for (int i = 0; i < 5; i++) {
            saveTelemetry(vehicle, base.plusMinutes(i));
        }

        // 다른 차량의 데이터는 조회 결과에서 제외되어야 함
        saveTelemetry(other, base.plusHours(1));

        PageRequest pageable = PageRequest.of(
                1,
                2,
                Sort.by(
                        Sort.Order.desc("timestamp"),
                        Sort.Order.desc("id")
                )
        );

        // when
        Page<Telemetry> page = telemetryRepository.findByVehicle_Id(
                vehicle.getId(),
                pageable
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(2);

        assertThat(page.getContent())
                .extracting(Telemetry::getTimestamp)
                .containsExactly(
                        base.plusMinutes(2),
                        base.plusMinutes(1)
                );
    }

    @Test
    @DisplayName("최신 텔레메트리 조회 시 시간이 같으면 ID가 큰 데이터를 반환한다")
    void shouldReturnHighestIdWhenTimestampsMatch() {
        // given
        Vehicle vehicle = saveVehicle("VEHICLE-001");

        LocalDateTime earlier = LocalDateTime.of(2026, 10, 30, 9, 0);
        LocalDateTime latestTime = earlier.plusHours(1);

        saveTelemetry(vehicle, earlier);
        saveTelemetry(vehicle, latestTime);

        Telemetry expected = saveTelemetry(vehicle, latestTime);

        // when
        Telemetry found = telemetryRepository
                .findTopByVehicle_IdOrderByTimestampDescIdDesc(vehicle.getId())
                .orElseThrow();

        // then
        assertThat(found.getId()).isEqualTo(expected.getId());
        assertThat(found.getTimestamp()).isEqualTo(latestTime);
    }

    @Test
    @DisplayName("텔레메트리가 없는 차량의 최신 조회 결과는 비어 있다")
    void shouldReturnEmptyWhenVehicleHasNoTelemetry() {
        // given
        Vehicle vehicle = saveVehicle("VEHICLE-001");

        // when & then
        assertThat(telemetryRepository
                .findTopByVehicle_IdOrderByTimestampDescIdDesc(vehicle.getId()))
                .isEmpty();
    }
}