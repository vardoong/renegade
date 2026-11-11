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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    private Telemetry saveTelemetry(Vehicle vehicle, Instant timestamp) {
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
        Instant timestamp = Instant.parse("2026-10-30T12:30:00Z");

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

        Instant base = Instant.parse("2026-10-30T12:30:00Z");

        for (int i = 0; i < 5; i++) {
            saveTelemetry(vehicle, base.plus(i, ChronoUnit.MINUTES));
        }

        // 다른 차량의 데이터는 조회 결과에서 제외되어야 함
        saveTelemetry(other, base.plusSeconds(3600));

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
                        base.plusSeconds(120),
                        base.plusSeconds(60)
                );
    }

    @Test
    @DisplayName("최신 텔레메트리 조회 시 시간이 같으면 ID가 큰 데이터를 반환한다")
    void shouldReturnHighestIdWhenTimestampsMatch() {
        // given
        Vehicle vehicle = saveVehicle("VEHICLE-001");

        Instant earlier = Instant.parse("2026-10-30T12:30:00Z");
        Instant latestTime = earlier.plus(1, ChronoUnit.DAYS);

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

    @Test
    @DisplayName("기간 조회는 from을 포함하고 to와 다른 차량을 제외한다")
    void shouldFilterByHalfOpenRangeAndVehicle() {
        Vehicle vehicle = saveVehicle("RANGE-001");
        Vehicle other = saveVehicle("RANGE-002");
        Instant from = Instant.parse("2026-11-11T00:00:00Z");
        Instant to = from.plusSeconds(60);
        saveTelemetry(vehicle, from.minusNanos(1000));
        Telemetry start = saveTelemetry(vehicle, from);
        Telemetry end = saveTelemetry(vehicle, to.minusNanos(1000));
        saveTelemetry(vehicle, to);
        saveTelemetry(other, from);

        Page<Telemetry> result = telemetryRepository
                .findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
                        vehicle.getId(), from, to, PageRequest.of(0, 20,
                                Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id"))));

        assertThat(result.getContent()).extracting(Telemetry::getId)
                .containsExactly(end.getId(), start.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(telemetryRepository
                .findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
                        vehicle.getId(), to.plusSeconds(1), to.plusSeconds(2), PageRequest.of(0, 20)))
                .isEmpty();
    }

    @Test
    @DisplayName("from만 있으면 하한을 포함하고 to만 있으면 상한을 제외한다")
    void shouldFilterWithSingleBound() {
        Vehicle vehicle = saveVehicle("BOUND-001");
        Vehicle other = saveVehicle("BOUND-002");
        Instant bound = Instant.parse("2026-11-11T00:00:00Z");
        Telemetry before = saveTelemetry(vehicle, bound.minusSeconds(1));
        Telemetry at = saveTelemetry(vehicle, bound);
        Telemetry after = saveTelemetry(vehicle, bound.plusSeconds(1));
        saveTelemetry(other, bound.minusSeconds(1));
        saveTelemetry(other, bound.plusSeconds(1));
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("timestamp").descending());

        assertThat(telemetryRepository.findByVehicle_IdAndTimestampGreaterThanEqual(
                vehicle.getId(), bound, pageable).getContent())
                .extracting(Telemetry::getId).containsExactly(after.getId(), at.getId());
        assertThat(telemetryRepository.findByVehicle_IdAndTimestampLessThan(
                vehicle.getId(), bound, pageable).getContent())
                .extracting(Telemetry::getId).containsExactly(before.getId());
    }

    @Test
    @DisplayName("기간 필터 후에도 전체 건수와 시간·ID 내림차순 페이지를 유지한다")
    void shouldPageFilteredResultsWithStableSort() {
        Vehicle vehicle = saveVehicle("PAGE-001");
        Instant from = Instant.parse("2026-11-11T00:00:00Z");
        Telemetry first = saveTelemetry(vehicle, from);
        Telemetry second = saveTelemetry(vehicle, from.plusSeconds(1));
        saveTelemetry(vehicle, from.plusSeconds(1));
        saveTelemetry(vehicle, from.plusSeconds(2));
        saveTelemetry(vehicle, from.minusSeconds(1));
        saveTelemetry(vehicle, from.plusSeconds(3));

        Page<Telemetry> result = telemetryRepository
                .findByVehicle_idAndTimestampGreaterThanEqualAndTimestampLessThan(
                        vehicle.getId(), from, from.plusSeconds(3), PageRequest.of(1, 2,
                                Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id"))));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent()).extracting(Telemetry::getId)
                .containsExactly(second.getId(), first.getId());
    }
}