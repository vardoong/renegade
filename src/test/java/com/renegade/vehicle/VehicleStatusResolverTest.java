package com.renegade.vehicle;

import com.renegade.telemetry.Telemetry;
import com.renegade.vehicle.enums.VehicleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleStatusResolverTest {

    private final Instant receivedAt = Instant.parse("2026-11-11T00:00:00Z");
    private final VehicleStatusResolver resolver = new VehicleStatusResolver(Duration.ofMinutes(3));

    @ParameterizedTest
    @CsvSource({"0,PARKED", "0.1,DRIVING", "60,DRIVING"})
    @DisplayName("3분 직전에는 최신 속도로 주행·주차 상태를 판정한다")
    void resolvesRecentStatus(double speed, VehicleStatus expected) {
        assertThat(resolver.resolve(telemetry(speed), receivedAt.plusSeconds(180).minusNanos(1)))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0,0", "60,0", "0,1", "60,1"})
    @DisplayName("정확히 3분 및 3분 초과이면 속도에 관계없이 OFFLINE이다")
    void resolvesOfflineAtAndAfterThreshold(double speed, long extraNanos) {
        assertThat(resolver.resolve(telemetry(speed), receivedAt.plusSeconds(180).plusNanos(extraNanos)))
                .isEqualTo(VehicleStatus.OFFLINE);
    }

    @Test
    @DisplayName("설정한 미수신 기준 시간을 적용한다")
    void usesConfiguredThreshold() {
        VehicleStatusResolver custom = new VehicleStatusResolver(Duration.ofMinutes(5));
        assertThat(custom.resolve(telemetry(60), receivedAt.plusSeconds(180)))
                .isEqualTo(VehicleStatus.DRIVING);
        assertThat(custom.resolve(telemetry(60), receivedAt.plusSeconds(300)))
                .isEqualTo(VehicleStatus.OFFLINE);
    }

    private Telemetry telemetry(double speed) {
        // 차량 측정 시각이 오래되어도 서버 수신 시각을 기준으로 판정해야 한다.
        Telemetry telemetry = Telemetry.builder()
                .speed(speed).timestamp(receivedAt.minus(Duration.ofDays(1))).build();
        ReflectionTestUtils.setField(telemetry, "createdAt", receivedAt);
        return telemetry;
    }
}
